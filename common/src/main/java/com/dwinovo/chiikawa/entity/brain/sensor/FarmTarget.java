package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.utils.Deliveries;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * The kinds of block the farmer sensor finds for the farmer's errands: what makes a block one, where the sensor
 * remembers it, and which errand works it. The one place that says so, for the sensor that finds them and for the view
 * that shows them to the errands.
 */
public enum FarmTarget {
    /** A crop that is ripe. */
    HARVEST(ErrandIds.HARVEST, InitMemory.HARVEST_POS, (level, pet, pos) -> Utils.canHarvesr(level, pos), pet -> true),
    /** Soil a seed in the pet's bag can be planted on, so none to look for while the pet has no seed. */
    PLANT(ErrandIds.PLANT, InitMemory.PLANT_POS, Utils::isPlantableBase, pet -> !Utils.getSeed(pet).isEmpty()),
    /** A container that would take something from the pet's bag, so none to look for while the bag has nothing to deliver. */
    DELIVER(ErrandIds.DELIVER, InitMemory.CONTAINER_POS,
        (level, pet, pos) -> Deliveries.worthVisiting(level, pos, pet.getBackpack()), pet -> Deliveries.hasItems(pet.getBackpack())),
    /** A wild plant to pull up. */
    WEED(ErrandIds.WEED, InitMemory.WEED_POS, (level, pet, pos) -> Utils.isWeed(level, pos), pet -> true),
    /** A mushroom to pick. */
    MUSHROOM(ErrandIds.PICK_MUSHROOM, InitMemory.MUSHROOM_POS, (level, pet, pos) -> Utils.isMushroom(level, pos), pet -> true);

    /** Whether a block is one of the kind, for a pet as it is. */
    @FunctionalInterface
    private interface Holds {
        boolean at(ServerLevel level, AbstractPet pet, BlockPos pos);
    }

    private final ResourceLocation errand;
    private final Supplier<MemoryModuleType<BlockPos>> memory;
    private final Holds holds;
    private final Predicate<AbstractPet> worthFinding;

    FarmTarget(ResourceLocation errand, Supplier<MemoryModuleType<BlockPos>> memory, Holds holds,
            Predicate<AbstractPet> worthFinding) {
        this.errand = errand;
        this.memory = memory;
        this.holds = holds;
        this.worthFinding = worthFinding;
    }

    /** @return the errand that works this kind of block, whose rests on a block the sensor skips */
    public ResourceLocation errand() {
        return errand;
    }

    /** @return the memory the sensor keeps the block it found in, which only the sensor writes */
    public MemoryModuleType<BlockPos> memory() {
        return memory.get();
    }

    /** @return whether the block is one of this kind, for the pet as it is: cheap block checks, no pathfinding */
    public boolean holds(ServerLevel level, AbstractPet pet, BlockPos pos) {
        return holds.at(level, pet, pos);
    }

    /** @return whether there is any use in looking for one, for the pet as it is */
    public boolean worthFinding(AbstractPet pet) {
        return worthFinding.test(pet);
    }

    /** @return the block the sensor remembers, if it still is one of this kind, where it is now */
    public Optional<GlobalPos> seenBy(AbstractPet pet) {
        return pet.level() instanceof ServerLevel level
            ? pet.getBrain().getMemory(memory()).filter(pos -> holds(level, pet, pos)).map(pos -> GlobalPos.of(level.dimension(), pos))
            : Optional.empty();
    }
}
