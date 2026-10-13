package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * The rest of a farmer's round: sowing what it has, putting the crop away, and picking up
 * what is lying about.
 *
 * <p>Harvesting is only the loud half of farming. A pet that reaps and then stands on the
 * harvest, or fills its own backpack and stops, is a pet an owner has to tidy up after —
 * which is the opposite of the point.
 */
@GameTestHolder(Constants.MOD_ID)
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

        helper.assertTrue(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), Component.literal("the seed was not planted"));
        helper.assertBlockPresent(Blocks.WHEAT, bed.above());
        helper.assertTrue(seeds.getCount() == 2, Component.literal("the seeds left are " + seeds.getCount()));

        helper.assertFalse(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), Component.literal("planted where a crop already stands"));
        helper.assertTrue(seeds.getCount() == 2, Component.literal("a seed was used up for nothing"));

        helper.setBlock(bed.above(), Blocks.STONE);
        helper.assertFalse(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), Component.literal("planted in solid stone"));
        helper.setBlock(bed.above(), Blocks.AIR);
        helper.assertTrue(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), Component.literal("the space was free again"));

        // The soil gone, as when it is broken just before the pet comes to plant: nothing to plant in.
        helper.setBlock(bed.above(), Blocks.AIR);
        helper.setBlock(bed, Blocks.AIR);
        helper.assertFalse(Utils.plantSeed(helper.getLevel(), pet, helper.absolutePos(bed), seeds), Component.literal("planted in the air"));
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
            Container container = helper.getBlockEntity(chest, ChestBlockEntity.class);
            helper.assertTrue(!container.isEmpty(), Component.literal("the chest is still empty; the pet was "
                + pet.getIntent().map(Object::toString).orElse("doing nothing")
                + " and had been told about a container at "
                + pet.getBrain().getMemory(InitMemory.CONTAINER_POS.get()).map(Object::toString).orElse("nowhere")));
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
            helper.assertFalse(carries(pet, Items.COBBLESTONE), Component.literal("the pet pocketed somebody's cobblestone"));
            helper.succeed();
        });
    }

    /** Wheat on the floor is wheat gone to waste; the pet picks it up. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_pet_picks_up_what_is_lying_about(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        helper.spawnItem(Items.WHEAT, new BlockPos(7, STAND, 4));

        helper.succeedWhen(() -> helper.assertTrue(carries(pet, Items.WHEAT),
            Component.literal("the wheat is still on the floor")));
    }
}
