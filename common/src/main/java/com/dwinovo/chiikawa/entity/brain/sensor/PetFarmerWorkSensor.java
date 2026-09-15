package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.utils.BlockSearch;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Single farmer work sensor — replaces the former plant / harvest / container
 * sensors, and also finds weeds and mushrooms. Three perf properties matter here:
 *
 * <ol>
 *   <li><b>No pathfinding in the scan.</b> Candidates are chosen by cheap block
 *       checks only. Reachability (an A* path) is verified once by the walk-to
 *       behavior; a position found unreachable is blacklisted on the pet
 *       ({@link AbstractPet#blacklistUnreachable}) so this scan skips it.</li>
 *   <li><b>Cheap re-validation.</b> Existing targets are re-checked with a block
 *       lookup, never a re-path.</li>
 *   <li><b>One pass.</b> A single spiral collects all needed targets at once, and
 *       the whole scan is skipped when every needed target is already set.</li>
 * </ol>
 *
 * Priority is harvest &gt; plant &gt; container, matching the order in which the
 * {@code harvest}, {@code plant} and {@code deliver} intents yield; weeds and
 * mushrooms are found independently. The sensor only perceives: it keeps scanning
 * whatever the owner's directive, the pet's ownership or the time of day, and the
 * intents decide whether a target is used.
 */
public class PetFarmerWorkSensor extends Sensor<AbstractPet> {
    private static final int MAX_RADIUS = 5;
    private static final int VERTICAL_RANGE = 1;
    /** Backoff cap: each consecutive empty search adds this many ticks, up to MAX_EMPTY_STREAK steps. */
    private static final int BACKOFF_STEP_TICKS = 30;
    // Caps idle search interval at ~3*30 = 90t (~4.5s) so a standing-still pet still
    // notices freshly-placed work reasonably fast.
    private static final int MAX_EMPTY_STREAK = 3;
    /**
     * A remembered target this far from the pet is forgotten. Intents ignore targets
     * outside their anchor's reach, and a kept target blocks the scan for another one,
     * so a pet that wandered off must not stay stuck on a target it no longer uses.
     */
    private static final double FORGET_DISTANCE = MAX_RADIUS * 2.0;

    // (6) Adaptive backoff state (per-pet: each brain owns its own sensor instance).
    private long nextSearchTime = 0L;
    private int emptySearchStreak = 0;
    private BlockPos lastSearchCenter = null;

    /**
     * Memory types managed by this sensor.
     * @return required memory types
     */
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            InitMemory.HARVEST_POS.get(),
            InitMemory.PLANT_POS.get(),
            InitMemory.CONTAINER_POS.get(),
            InitMemory.WEED_POS.get(),
            InitMemory.MUSHROOM_POS.get()
        );
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        // Only farmers use these targets; skip the scan for other jobs.
        if (pet.getPetJobId() != InitRegistry.FARMER_ID) {
            brain.eraseMemory(InitMemory.HARVEST_POS.get());
            brain.eraseMemory(InitMemory.PLANT_POS.get());
            brain.eraseMemory(InitMemory.CONTAINER_POS.get());
            brain.eraseMemory(InitMemory.WEED_POS.get());
            brain.eraseMemory(InitMemory.MUSHROOM_POS.get());
            return;
        }

        boolean hasSeed = !Utils.getSeed(pet).isEmpty();
        boolean hasDeliverItem = hasDeliverItem(pet.getBackpack());

        // (2) Re-validate existing targets cheaply — block checks only, no pathfinding.
        revalidateBlock(level, pet, InitMemory.HARVEST_POS.get(), Utils::canHarvesr);
        revalidatePlant(level, pet);
        revalidateBlock(level, pet, InitMemory.WEED_POS.get(), Utils::isWeed);
        revalidateBlock(level, pet, InitMemory.MUSHROOM_POS.get(), Utils::isMushroom);

        boolean hasHarvest = brain.getMemory(InitMemory.HARVEST_POS.get()).isPresent();
        boolean hasPlant = brain.getMemory(InitMemory.PLANT_POS.get()).isPresent();

        // Container delivery only matters when there's nothing to harvest or plant.
        if (hasHarvest || hasPlant || !hasDeliverItem) {
            brain.eraseMemory(InitMemory.CONTAINER_POS.get());
        } else {
            revalidateContainer(level, pet);
        }
        boolean hasContainer = brain.getMemory(InitMemory.CONTAINER_POS.get()).isPresent();

        // Decide what still needs finding (priority order).
        boolean needHarvest = !hasHarvest;
        boolean needPlant = hasSeed && !hasHarvest && !hasPlant;
        boolean needContainer = hasDeliverItem && !hasHarvest && !hasPlant && !hasContainer;
        boolean needWeed = brain.getMemory(InitMemory.WEED_POS.get()).isEmpty();
        boolean needMushroom = brain.getMemory(InitMemory.MUSHROOM_POS.get()).isEmpty();
        if (!needHarvest && !needPlant && !needContainer && !needWeed && !needMushroom) {
            return; // everything satisfied — skip the scan entirely
        }

        // (6) Adaptive backoff: if recent searches from this spot found nothing,
        // search progressively less often. Reset the moment the pet moves.
        BlockPos center = pet.blockPosition();
        if (!center.equals(lastSearchCenter)) {
            lastSearchCenter = center;
            emptySearchStreak = 0;
            nextSearchTime = 0L;
        }
        if (level.getGameTime() < nextSearchTime) {
            return;
        }

        // (1)(3) One spiral pass, cheap predicates only, skipping blacklisted positions.
        BlockPos[] found = new BlockPos[5]; // 0 = harvest, 1 = plant, 2 = container, 3 = weed, 4 = mushroom
        BlockSearch.spiralVisit(pet.blockPosition(), MAX_RADIUS, VERTICAL_RANGE, pos -> {
            if (pet.isReachBlacklisted(pos)) {
                return false;
            }
            if (needHarvest && found[0] == null && Utils.canHarvesr(level, pos)) {
                found[0] = pos.immutable();
            }
            if (needPlant && found[1] == null && Utils.isPlantableBase(level, pet, pos)) {
                found[1] = pos.immutable();
            }
            if (needContainer && found[2] == null
                && isDeliverContainer(level, pos) && canInsertContainer(level, pos, pet)) {
                found[2] = pos.immutable();
            }
            if (needWeed && found[3] == null && Utils.isWeed(level, pos)) {
                found[3] = pos.immutable();
            }
            if (needMushroom && found[4] == null && Utils.isMushroom(level, pos)) {
                found[4] = pos.immutable();
            }
            return (!needHarvest || found[0] != null)
                && (!needPlant || found[1] != null)
                && (!needContainer || found[2] != null)
                && (!needWeed || found[3] != null)
                && (!needMushroom || found[4] != null);
        });

        if (needHarvest && found[0] != null) {
            brain.setMemory(InitMemory.HARVEST_POS.get(), found[0]);
        }
        if (needPlant && found[1] != null) {
            brain.setMemory(InitMemory.PLANT_POS.get(), found[1]);
        }
        if (needContainer && found[2] != null) {
            brain.setMemory(InitMemory.CONTAINER_POS.get(), found[2]);
        }
        if (needWeed && found[3] != null) {
            brain.setMemory(InitMemory.WEED_POS.get(), found[3]);
        }
        if (needMushroom && found[4] != null) {
            brain.setMemory(InitMemory.MUSHROOM_POS.get(), found[4]);
        }

        // Found work → search eagerly again; found nothing → back off a step.
        boolean foundAny = (needHarvest && found[0] != null)
            || (needPlant && found[1] != null)
            || (needContainer && found[2] != null)
            || (needWeed && found[3] != null)
            || (needMushroom && found[4] != null);
        if (foundAny) {
            emptySearchStreak = 0;
            nextSearchTime = 0L;
        } else {
            emptySearchStreak = Math.min(emptySearchStreak + 1, MAX_EMPTY_STREAK);
            nextSearchTime = level.getGameTime() + (long) emptySearchStreak * BACKOFF_STEP_TICKS;
        }
    }

    private static void revalidateBlock(ServerLevel level, AbstractPet pet, MemoryModuleType<BlockPos> memory,
            BiPredicate<ServerLevel, BlockPos> qualifies) {
        Optional<BlockPos> opt = pet.getBrain().getMemory(memory);
        if (opt.isPresent()) {
            BlockPos pos = opt.get();
            if (isStale(pet, pos) || !qualifies.test(level, pos)) {
                pet.getBrain().eraseMemory(memory);
            }
        }
    }

    private static void revalidatePlant(ServerLevel level, AbstractPet pet) {
        Optional<BlockPos> opt = pet.getBrain().getMemory(InitMemory.PLANT_POS.get());
        if (opt.isPresent()) {
            BlockPos pos = opt.get();
            if (isStale(pet, pos) || !Utils.isPlantableBase(level, pet, pos)) {
                pet.getBrain().eraseMemory(InitMemory.PLANT_POS.get());
            }
        }
    }

    private static void revalidateContainer(ServerLevel level, AbstractPet pet) {
        Optional<BlockPos> opt = pet.getBrain().getMemory(InitMemory.CONTAINER_POS.get());
        if (opt.isPresent()) {
            BlockPos pos = opt.get();
            if (isStale(pet, pos)
                || !isDeliverContainer(level, pos)
                || !canInsertContainer(level, pos, pet)) {
                pet.getBrain().eraseMemory(InitMemory.CONTAINER_POS.get());
            }
        }
    }

    private static boolean isStale(AbstractPet pet, BlockPos pos) {
        return pet.isReachBlacklisted(pos) || !pos.closerThan(pet.blockPosition(), FORGET_DISTANCE);
    }

    private static boolean isDeliverContainer(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(InitTag.ENTITY_DELEVER_CONTAINER);
    }

    private static boolean hasDeliverItem(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.is(InitTag.ENTITY_DELIVER_ITEMS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canInsertContainer(ServerLevel level, BlockPos pos, AbstractPet pet) {
        Container backpack = pet.getBackpack();
        if (Services.ITEM_TRANSFER.hasBlockStorage(level, pos)) {
            for (int i = 0; i < backpack.getContainerSize(); i++) {
                ItemStack stack = backpack.getItem(i);
                if (stack.isEmpty() || !stack.is(InitTag.ENTITY_DELIVER_ITEMS)) {
                    continue;
                }
                ItemStack testStack = stack.copy();
                testStack.setCount(1);
                if (Services.ITEM_TRANSFER.insertIntoBlock(level, pos, testStack, true) > 0) {
                    return true;
                }
            }
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }
        for (int i = 0; i < backpack.getContainerSize(); i++) {
            ItemStack stack = backpack.getItem(i);
            if (stack.isEmpty() || !stack.is(InitTag.ENTITY_DELIVER_ITEMS)) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack slotStack = container.getItem(slot);
                if (slotStack.isEmpty()) {
                    return true;
                }
                if (ItemStack.isSameItemSameComponents(slotStack, stack)
                    && slotStack.getCount() < slotStack.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }
}
