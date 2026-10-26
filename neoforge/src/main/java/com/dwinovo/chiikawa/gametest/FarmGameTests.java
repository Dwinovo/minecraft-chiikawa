package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rest of a farmer's round: sowing what it has, putting the crop away, and picking up
 * what is lying about.
 *
 * <p>Harvesting is only the loud half of farming. A pet that reaps and then stands on the
 * harvest, or fills its own backpack and stops, is a pet an owner has to tidy up after —
 * which is the opposite of the point.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FarmGameTests {
    private static final String BATCH = "chiikawa_farm";
    /**
     * Long enough for a pet to notice, cross a few blocks and finish. Work is chosen
     * against everything else a pet might do, so a case that watches for it wants room
     * for a stroll on the way rather than a tight clock.
     */
    private static final int WORK_TICKS = 3600;

    /** How long a pet is watched leaving something alone before we believe it means to. */
    private static final int LEAVE_IT_TICKS = 600;

    /** The longest the farm behaviors took to find that they had no way to a crop: the walk to it lasted fifteen ticks. */
    private static final int GIVE_UP_TICKS = 15;
    /**
     * The longest a pet takes to reap a crop a few blocks off from the decision to go for it: it takes about twenty when
     * the crop is three blocks away, and a walk of the longest the sensor lets it set off on takes about forty.
     */
    private static final int REAP_TICKS = 60;

    private static final int STAND = 2;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Bare farmland and seeds in the bag: the farmer sows it. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_sows_empty_farmland(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.WHEAT_SEEDS, 8));

        BlockPos bed = new BlockPos(7, STAND - 1, 4);
        // Watered, or the farmland dries out and turns back to dirt mid-case.
        helper.setBlock(bed, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));

        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WHEAT, bed.above()));
    }

    /** Not only wheat: a farmer sows beetroot seeds, potatoes and carrots as well. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_sows_beetroot_seeds(GameTestHelper helper) {
        sows(helper, Items.BEETROOT_SEEDS, Blocks.BEETROOTS, Blocks.FARMLAND.defaultBlockState()
            .setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
    }

    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_plants_potatoes(GameTestHelper helper) {
        sows(helper, Items.POTATO, Blocks.POTATOES, Blocks.FARMLAND.defaultBlockState()
            .setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
    }

    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_plants_carrots(GameTestHelper helper) {
        sows(helper, Items.CARROT, Blocks.CARROTS, Blocks.FARMLAND.defaultBlockState()
            .setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
    }

    /** Nether wart goes on soul sand, not farmland. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_replants_nether_wart_on_soul_sand(GameTestHelper helper) {
        sows(helper, Items.NETHER_WART, Blocks.NETHER_WART, Blocks.SOUL_SAND.defaultBlockState());
    }

    private static void sows(GameTestHelper helper, net.minecraft.world.item.Item seed, net.minecraft.world.level.block.Block crop,
            net.minecraft.world.level.block.state.BlockState soil) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(seed, 8));
        BlockPos bed = new BlockPos(7, STAND - 1, 4);
        helper.setBlock(bed, soil);

        helper.succeedWhen(() -> helper.assertBlockPresent(crop, bed.above()));
    }

    /**
     * Planting needs no player behind it: put straight into the world, one seed used up, for any
     * seed and with nobody to ask where they are looking. A space already taken, or soil that is gone
     * as when it was broken in the moment before the pet came to plant, is left alone and the seed kept.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 100)
    public static void planting_needs_no_player_and_leaves_a_taken_space_alone(GameTestHelper helper) {
        AbstractPet pet = worker(helper, new BlockPos(4, STAND, 4));
        BlockPos bed = new BlockPos(7, STAND - 1, 4);
        helper.setBlock(bed, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS, 3);

        helper.assertTrue(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), "the seed was not planted");
        helper.assertBlockPresent(Blocks.WHEAT, bed.above());
        helper.assertTrue(seeds.getCount() == 2, "the seeds left are " + seeds.getCount());

        helper.assertFalse(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), "planted where a crop already stands");
        helper.assertTrue(seeds.getCount() == 2, "a seed was used up for nothing");

        helper.setBlock(bed.above(), Blocks.STONE);
        helper.assertFalse(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), "planted in solid stone");
        helper.setBlock(bed.above(), Blocks.AIR);
        helper.assertTrue(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), "the space was free again");

        // The soil gone, as when it is broken just before the pet comes to plant: nothing to plant in.
        helper.setBlock(bed.above(), Blocks.AIR);
        helper.setBlock(bed, Blocks.AIR);
        helper.assertFalse(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), "planted in the air");
        helper.succeed();
    }

    /** A chest within reach is where the crop goes, not the pet's own bag. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_puts_the_crop_in_a_chest(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.WHEAT, 12));

        BlockPos chest = new BlockPos(7, STAND, 4);
        helper.setBlock(chest, Blocks.CHEST);

        helper.succeedWhen(() -> {
            Container container = (Container) helper.getBlockEntity(chest);
            helper.assertTrue(!container.isEmpty(), "the chest is still empty; the pet was "
                + pet.getIntent().map(Object::toString).orElse("doing nothing")
                + " and had been told about a container at "
                + pet.getBrain().getMemory(InitMemory.WORK_SIGHTS.get()).map(sights -> sights.get(WorkData.DELIVER))
                    .map(Object::toString).orElse("nowhere"));
        });
    }

    /**
     * The harvest is carried to the chest once the field is reaped, not after every crop: a farmer beside a field of ripe
     * wheat knows the next crop the moment it has reaped one, so it is never between two crops with nothing to harvest
     * and a chest to run to. The case reaps nine crops, and the pet may set off for the chest only when none is left ripe.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_reaps_the_field_before_it_carries_the_harvest_away(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        List<BlockPos> field = new ArrayList<>();
        for (int x = 6; x <= 8; x++) {
            for (int z = 3; z <= 5; z++) {
                field.add(new BlockPos(x, STAND, z));
                ripeWheat(helper, field.get(field.size() - 1));
            }
        }
        BlockPos chest = new BlockPos(4, STAND, 8);
        helper.setBlock(chest, Blocks.CHEST);
        boolean[] setOffForTheChest = {false};

        helper.onEachTick(() -> {
            if (!setOffForTheChest[0] && pet.getIntent().filter(WorkData.DELIVER::equals).isPresent()) {
                setOffForTheChest[0] = true;
                long ripe = field.stream().filter(crop -> helper.getBlockState(crop).getBlock() == Blocks.WHEAT
                    && helper.getBlockState(crop).getValue(CropBlock.AGE) == CropBlock.MAX_AGE).count();
                helper.assertTrue(ripe == 0, "the pet set off for the chest with " + ripe + " ripe crops still in the field");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(setOffForTheChest[0], "the pet never set off for the chest");
            Container container = (Container) helper.getBlockEntity(chest);
            helper.assertTrue(container.hasAnyMatching(stack -> stack.is(Items.WHEAT)), "the chest has no wheat in it");
        });
    }

    /**
     * Cobblestone on the floor stays there. A pet that hoovered up everything loose would
     * empty a player's dropped inventory into its own bag, which is a way to lose things.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_pet_leaves_alone_what_is_not_its_business(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        helper.spawnItem(Items.COBBLESTONE, new BlockPos(7, STAND, 4));

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertFalse(carries(pet, Items.COBBLESTONE), "the pet pocketed somebody's cobblestone");
            helper.succeed();
        });
    }

    /** Wheat on the floor is wheat gone to waste; the pet picks it up. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_pet_picks_up_what_is_lying_about(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        helper.spawnItem(Items.WHEAT, new BlockPos(7, STAND, 4));

        helper.succeedWhen(() -> helper.assertTrue(carries(pet, Items.WHEAT),
            "the wheat is still on the floor"));
    }

    /**
     * A bag with no room for the wheat is no reason to walk to it: the pet is not offered what it cannot take, so it
     * does not set off for it and stand at it, or give up and set off again.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_pet_with_a_full_bag_is_not_sent_for_what_is_lying_about(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            pet.getBackpack().setItem(slot, new ItemStack(Items.COBBLESTONE, Items.COBBLESTONE.getDefaultMaxStackSize()));
        }
        ItemEntity wheat = helper.spawnItem(Items.WHEAT, new BlockPos(7, STAND, 4));

        helper.onEachTick(() -> helper.assertFalse(pet.getIntent().filter(ErrandIds.PICK_UP_ITEM::equals).isPresent(),
            "the pet set off for the wheat with no room for it"));
        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertTrue(wheat.isAlive(), "the wheat was taken");
            helper.succeed();
        });
    }

    /**
     * A crop walled in on every side is one the pet cannot get to. It gives up on it, and leaves it alone for a while
     * instead of standing at the wall for good: the next crop, farther off and open, is reaped.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_crop_the_pet_cannot_get_to_does_not_hold_it_back_from_one_it_can(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos walled = new BlockPos(7, STAND, 4);
        ripeWheat(helper, walled);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    if (x != 0 || z != 0) {
                        helper.setBlock(walled.offset(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        BlockPos open = new BlockPos(4, STAND, 8);
        ripeWheat(helper, open);

        helper.succeedWhen(() -> {
            helper.assertTrue(carries(pet, Items.WHEAT), "the pet never came away with the wheat that was open to it");
            helper.assertTrue(helper.getBlockState(open).getValue(CropBlock.AGE) < CropBlock.MAX_AGE, "the open crop was not reaped");
            helper.assertTrue(helper.getBlockState(walled).getValue(CropBlock.AGE) == CropBlock.MAX_AGE, "the walled-in crop was reaped");
        });
    }

    /**
     * A pet gives up on a crop it has no way to the moment the brain's mover says so, not a long while after: the farm
     * behaviors that pathfound for it before they set off took up to fifteen ticks, and a pet that waited out a hundred
     * ticks of having no way would make a field with several walled-in crops look like a pet that is stuck. Then, with a
     * crop it can get to in range, it is reaping that within the time a short walk takes from the decision to go for it.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_pet_gives_up_on_a_crop_it_cannot_get_to_at_once(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos walled = new BlockPos(7, STAND, 4);
        ripeWheat(helper, walled);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    if (x != 0 || z != 0) {
                        helper.setBlock(walled.offset(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        BlockPos open = new BlockPos(4, STAND, 7);
        boolean[] gaveUp = {false};
        long[] setOff = {-1L};

        helper.onEachTick(() -> {
            if (!gaveUp[0]) {
                Optional<DecisionLog.Ended> ended = harvestEnded(pet);
                if (ended.isEmpty()) {
                    return;
                }
                helper.assertTrue(ended.get().outcome() instanceof Outcome.GaveUp,
                    "the pet ended its walk to the walled-in crop " + ended.get().outcome());
                helper.assertTrue(ended.get().ranTicks() <= GIVE_UP_TICKS,
                    "the pet took " + ended.get().ranTicks() + " ticks to give up on a crop it had no way to");
                gaveUp[0] = true;
                ripeWheat(helper, open);
                return;
            }
            if (setOff[0] < 0L && pet.getIntent().filter(WorkData.HARVEST::equals).isPresent()) {
                setOff[0] = helper.getTick();
            }
            if (carries(pet, Items.WHEAT)) {
                helper.succeed();
            }
            helper.assertTrue(setOff[0] < 0L || helper.getTick() - setOff[0] <= REAP_TICKS,
                "the pet had not reaped the open crop " + REAP_TICKS + " ticks after it set off for it");
        });
    }

    private static Optional<DecisionLog.Ended> harvestEnded(AbstractPet pet) {
        return pet.getMind().log().entries().stream()
            .filter(entry -> entry instanceof DecisionLog.Ended ended && ended.errand().equals(WorkData.HARVEST))
            .map(DecisionLog.Ended.class::cast).findFirst();
    }

    private static void ripeWheat(GameTestHelper helper, BlockPos crop) {
        helper.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        BlockState ripe = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        helper.setBlock(crop, ripe);
    }
}
