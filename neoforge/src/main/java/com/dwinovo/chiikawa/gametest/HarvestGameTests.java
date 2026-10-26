package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.step.Harvest;
import com.dwinovo.chiikawa.entity.brain.errand.step.LiveHandle;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The one step that breaks a block into the pet's bag, whether the block is a ripe crop, a weed or a mushroom. The step
 * is ticked by hand on a pet that does not think, so that what it did is all there is to see.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HarvestGameTests {
    private static final String BATCH = "chiikawa_harvest";
    private static final int STAND = 2;

    /** A weed or a mushroom, with the work it counts as and what says it is one. */
    private record Forage(Block block, ResourceLocation counter, BiPredicate<ServerLevel, BlockPos> holds) {
    }

    private static final List<Forage> FORAGE = List.of(
        new Forage(Blocks.SHORT_GRASS, PetWorkCounters.WEED, Utils::isWeed),
        new Forage(Blocks.TALL_GRASS, PetWorkCounters.WEED, Utils::isWeed),
        new Forage(Blocks.FERN, PetWorkCounters.WEED, Utils::isWeed),
        new Forage(Blocks.LARGE_FERN, PetWorkCounters.WEED, Utils::isWeed),
        new Forage(Blocks.RED_MUSHROOM, PetWorkCounters.PICK_MUSHROOM, Utils::isMushroom),
        new Forage(Blocks.BROWN_MUSHROOM, PetWorkCounters.PICK_MUSHROOM, Utils::isMushroom));

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A weed or a mushroom goes through the crop handler and comes out as it did when the step that picked it broke
     * it without one: the same drops in the bag, the same blocks left standing, the shears not worn, and the work counted
     * towards the slip once. The step before the crop handler is written out here as it was, drops into the bag and
     * the block destroyed without drops of its own, on a block of the same kind a few blocks away.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 40)
    public static void a_weed_or_a_mushroom_harvested_is_what_picking_it_without_the_handler_was(GameTestHelper helper) {
        // The shears make the drops of grass and ferns certain, instead of a seed now and then.
        AbstractPet pet = thoughtless(holding(worker(helper, new BlockPos(2, STAND, 2)), Items.SHEARS));
        LiveHandle handle = new LiveHandle(pet);
        ServerLevel level = helper.getLevel();

        int row = 0;
        for (Forage forage : FORAGE) {
            BlockPos picked = helper.absolutePos(new BlockPos(6, STAND, 2 + 2 * row));
            BlockPos harvested = helper.absolutePos(new BlockPos(10, STAND, 2 + 2 * row));
            row++;
            put(level, forage.block(), picked);
            put(level, forage.block(), harvested);
            String name = forage.block().getDescriptionId();

            emptyBag(pet);
            BlockState state = level.getBlockState(picked);
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(picked) : null;
            pet.dropResourcesToPetInv(state, level, picked, blockEntity, pet.getMainHandItem());
            level.destroyBlock(picked, false, pet);
            Map<String, Integer> pickedBag = bag(pet);

            emptyBag(pet);
            pet.setTask(new PetTask(PetTaskTypeData.WEEDING, farmer(), forage.counter(), PetTask.NO_ICON, 100,
                PetTaskTypeData.reward(PetTaskTypeData.WEEDING), 0));
            StepStatus status = new Harvest(harvested, forage.holds(), Optional.of(forage.counter()), 0).tick(handle);

            helper.assertTrue(status.equals(StepStatus.DONE), name + ": the step ended " + status);
            helper.assertFalse(pickedBag.isEmpty(), name + ": picking it put nothing in the bag");
            helper.assertTrue(bag(pet).equals(pickedBag), name + ": the bag has " + bag(pet) + ", picked it had " + pickedBag);
            helper.assertTrue(level.getBlockState(harvested).isAir() && level.getBlockState(picked).isAir(),
                name + ": the block is not gone");
            helper.assertTrue(level.getBlockState(harvested.above()).isAir() && level.getBlockState(picked.above()).isAir(),
                name + ": the upper half is not gone");
            helper.assertTrue(pet.getTask().orElseThrow().progress() == 1, name + ": the work was not counted once");
            helper.assertTrue(pet.getMainHandItem().getDamageValue() == 0, name + ": the shears wore");
        }

        BlockPos nothing = helper.absolutePos(new BlockPos(14, STAND, 2));
        helper.assertTrue(new Harvest(nothing, Utils::isWeed, Optional.of(PetWorkCounters.WEED), 0).tick(handle)
            .equals(StepStatus.gaveUp("gone")), "a step on a place with no weed did not give up");
        helper.succeed();
    }

    /**
     * A ripe crop is reaped into the bag and put back as a young plant, and wears the tool in the pet's hand by as
     * much as the work says; a work that says none leaves it as it was.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 40)
    public static void a_ripe_crop_harvested_is_replanted_and_wears_the_tool_as_the_work_says(GameTestHelper helper) {
        AbstractPet pet = thoughtless(holding(worker(helper, new BlockPos(2, STAND, 2)), Items.WOODEN_HOE));
        LiveHandle handle = new LiveHandle(pet);
        ServerLevel level = helper.getLevel();
        BlockPos worn = helper.absolutePos(new BlockPos(6, STAND, 2));
        BlockPos spared = helper.absolutePos(new BlockPos(6, STAND, 4));
        for (BlockPos crop : new BlockPos[] {worn, spared}) {
            level.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(CropBlock.MAX_AGE));
        }

        helper.assertTrue(new Harvest(worn, Utils::canHarvesr, Optional.empty(), 1).tick(handle).equals(StepStatus.DONE),
            "the crop was not reaped");
        helper.assertTrue(pet.getMainHandItem().getDamageValue() == 1, "the hoe wore " + pet.getMainHandItem().getDamageValue());
        helper.assertTrue(level.getBlockState(worn).is(Blocks.WHEAT) && !((CropBlock) Blocks.WHEAT).isMaxAge(level.getBlockState(worn)),
            "the crop was not put back young");
        helper.assertTrue(bag(pet).getOrDefault(Items.WHEAT.getDescriptionId(), 0) == 1, "the bag has " + bag(pet));

        helper.assertTrue(new Harvest(spared, Utils::canHarvesr, Optional.empty(), 0).tick(handle).equals(StepStatus.DONE),
            "the second crop was not reaped");
        helper.assertTrue(pet.getMainHandItem().getDamageValue() == 1, "a work with no wear wore the hoe");
        helper.succeed();
    }

    /** A pet whose mind is not ticked, so that nothing but the step touches the world or the bag. */
    private static AbstractPet thoughtless(AbstractPet pet) {
        pet.setNoAi(true);
        return pet;
    }

    private static ResourceLocation farmer() {
        return InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FARMER.get());
    }

    /** Puts the block where it grows, on grass, a tall plant with both its halves. */
    private static void put(ServerLevel level, Block block, BlockPos pos) {
        level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        if (block instanceof DoublePlantBlock) {
            DoublePlantBlock.placeAt(level, block.defaultBlockState(), pos, Block.UPDATE_ALL);
        } else {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
        }
    }

    /** Takes everything out of the bag but the tool in the pet's hand, which is the bag's first slot. */
    private static void emptyBag(AbstractPet pet) {
        for (int slot = AbstractPet.MAINHAND_SLOT + 1; slot < pet.getBackpack().getContainerSize(); slot++) {
            pet.getBackpack().setItem(slot, ItemStack.EMPTY);
        }
    }

    /** @return what the pet's bag holds apart from the tool in its hand, by item and count */
    private static Map<String, Integer> bag(AbstractPet pet) {
        Map<String, Integer> held = new TreeMap<>();
        for (int slot = AbstractPet.MAINHAND_SLOT + 1; slot < pet.getBackpack().getContainerSize(); slot++) {
            ItemStack stack = pet.getBackpack().getItem(slot);
            if (!stack.isEmpty()) {
                held.merge(stack.getDescriptionId(), stack.getCount(), Integer::sum);
            }
        }
        return held;
    }
}
