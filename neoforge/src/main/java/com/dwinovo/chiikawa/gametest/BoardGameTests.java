package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.task.BoardSlot;
import com.dwinovo.chiikawa.task.PetTaskTypes;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The labor board, end to end: a pet that may not weed for free walks over, takes the
 * day's slip, does the work and comes away with emeralds.
 *
 * <p>This is the one case that has to hold for the whole of the mod's economy to mean
 * anything, and it is the one with the most between the first tick and the last — seeing,
 * reserving, walking, claiming, counting the work, being paid.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BoardGameTests {
    private static final String BATCH = "chiikawa_board";
    /** Everything above, at the speed a pet does it. */
    private static final int WORK_TICKS = 6000;

    private static final int STAND = 2;
    /** Grass to pull: more than the largest weeding slip asks for, so the work cannot run out. */
    private static final int WEED_PATCH = 6;
    /** How long two pets are watched, which is longer than one takes to walk over and claim. */
    private static final int WATCH_TICKS = 1200;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** From a bare hoe to a handful of emeralds, with nothing said to the pet along the way. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_board_pays_a_farmer_for_pulling_weeds(GameTestHelper helper) {
        BlockPos board = weedingBoard(helper);
        helper.setBlock(board, InitBlocks.LABOR_BOARD.get());

        weedPatch(helper);

        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        helper.succeedWhen(() -> helper.assertTrue(count(pet, Items.EMERALD) > 0,
            "the farmer never came away with anything for its trouble"));
    }

    /**
     * Paid and straight back for more. The loop only means anything if it is a loop: a pet
     * that does one job and then stands about for the rest of the day is a pet an owner has
     * to nudge, which is the thing the board was for.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_goes_back_for_the_next_slip(GameTestHelper helper) {
        BlockPos board = boardWithASecondFarmerSlip(helper);
        helper.setBlock(board, InitBlocks.LABOR_BOARD.get());
        weedPatch(helper);

        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        AtomicBoolean paid = new AtomicBoolean();
        helper.succeedWhen(() -> {
            if (count(pet, Items.EMERALD) > 0) {
                paid.set(true);
            }
            helper.assertTrue(paid.get(), "the farmer has not been paid for the first slip yet");
            // Being paid clears the slip, so one in hand afterwards is a second one.
            helper.assertTrue(pet.getTask().isPresent(), "the farmer stopped after one job");
        });
    }

    /** Grass over the pet's corner of the floor, enough for any weeding slip the board rolls. */
    private static void weedPatch(GameTestHelper helper) {
        for (int x = 0; x < WEED_PATCH; x++) {
            for (int z = 0; z < WEED_PATCH; z++) {
                BlockPos weed = new BlockPos(2 + x, STAND, 2 + z);
                helper.setBlock(weed.below(), Blocks.GRASS_BLOCK);
                helper.setBlock(weed, Blocks.SHORT_GRASS);
            }
        }
    }

    /**
     * Paid with nowhere to put it. A pet whose bag is full still gets what it earned — it
     * lands at its feet rather than quietly going nowhere, which is the difference between
     * a full bag and a robbery.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_with_a_full_bag_is_paid_at_its_feet(GameTestHelper helper) {
        helper.setBlock(weedingBoard(helper), InitBlocks.LABOR_BOARD.get());
        weedPatch(helper);

        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        // Everything but the hand it holds the hoe in, packed with something an emerald
        // cannot join and a farmer will not pick up again.
        for (int slot = 1; slot < pet.getBackpack().getContainerSize(); slot++) {
            pet.getBackpack().setItem(slot, new ItemStack(Items.COBBLESTONE, Items.COBBLESTONE.getDefaultMaxStackSize()));
        }

        helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.EMERALD, new BlockPos(4, STAND, 4), 10.0));
    }

    /**
     * One slip, two farmers: one of them gets it and the other does not. A slip taken twice
     * is work paid for twice, and a pair of pets walking to the same board and both coming
     * away with the same job is exactly what the reservation is there to stop.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 200)
    public static void two_farmers_cannot_take_the_same_slip(GameTestHelper helper) {
        helper.setBlock(boardWithOneFarmerSlip(helper), InitBlocks.LABOR_BOARD.get());
        weedPatch(helper);

        AbstractPet first = holding(worker(helper, new BlockPos(3, STAND, 4)), Items.WOODEN_HOE);
        AbstractPet second = holding(worker(helper, new BlockPos(5, STAND, 4)), Items.WOODEN_HOE);
        AtomicBoolean taken = new AtomicBoolean();

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> {
                boolean firstHasOne = first.getTask().isPresent();
                boolean secondHasOne = second.getTask().isPresent();
                helper.assertFalse(firstHasOne && secondHasOne,
                    "both farmers walked away with the day's only slip");
                if (firstHasOne || secondHasOne) {
                    taken.set(true);
                }
            })
            .thenExecute(() -> helper.assertTrue(taken.get(), "neither farmer ever took the slip"))
            .thenSucceed();
    }

    /**
     * Where to put the board so that the first thing it offers a farmer today is weeding.
     *
     * <p>A board's day is rolled from the world seed, the day and its own position, so what
     * it offers is settled before anything is placed. Rather than reach into the block and
     * plant a slip — which would have the case testing a board nobody else could get — this
     * asks the same roll the board will ask, at each spot in turn, until it finds one that
     * offers the work this case is about.
     *
     * <p>The <i>first</i> farmer's slip, not any of them: picking mushrooms is a farmer's
     * work too, and a pet takes the first slip it can rather than the one a case had in
     * mind. A board that offers that first leaves the pet holding a slip it cannot finish
     * at noon, and the case waits out its clock for nothing.
     */
    private static BlockPos weedingBoard(GameTestHelper helper) {
        return boardWhere(helper, farmerSlips -> !farmerSlips.isEmpty()
            && farmerSlips.get(0).slip().type().equals(PetTaskTypeData.WEEDING),
            "offers a farmer weeding first");
    }

    /**
     * A board offering a farmer weeding and then something else. Without that second slip
     * there is nothing to go back for, and the case fails for want of work rather than for
     * want of a pet willing to do it.
     */
    private static BlockPos boardWithASecondFarmerSlip(GameTestHelper helper) {
        return boardWhere(helper, farmerSlips -> farmerSlips.size() >= 2
            && farmerSlips.get(0).slip().type().equals(PetTaskTypeData.WEEDING),
            "offers a farmer weeding and another slip after it");
    }

    /** A board with one slip for a farmer and no second one to fall back on. */
    private static BlockPos boardWithOneFarmerSlip(GameTestHelper helper) {
        return boardWhere(helper, farmerSlips -> farmerSlips.size() == 1
            && farmerSlips.get(0).slip().type().equals(PetTaskTypeData.WEEDING),
            "offers a farmer exactly one slip, for weeding");
    }

    /**
     * The first spot on the floor whose day, rolled, answers {@code wanted}.
     *
     * @param wanted asked of the day's slips a farmer could take, in the order a pet meets them
     */
    private static BlockPos boardWhere(GameTestHelper helper, Predicate<List<BoardSlot>> wanted, String what) {
        ServerLevel level = helper.getLevel();
        long day = level.getDayTime() / Level.TICKS_PER_DAY;
        ResourceLocation farmer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FARMER.get());
        for (int x = 8; x < 15; x++) {
            for (int z = 2; z < 15; z++) {
                BlockPos rel = new BlockPos(x, STAND, z);
                long seed = BoardSlips.seed(level.getSeed(), day, helper.absolutePos(rel));
                List<BoardSlot> farmerSlips = BoardSlips.roll(seed, PetTaskTypes.all()).stream()
                    .filter(slot -> slot.slip().capability().equals(farmer))
                    .toList();
                if (wanted.test(farmerSlips)) {
                    return rel;
                }
            }
        }
        throw new AssertionError("nowhere on this floor " + what + " today — "
            + "the roll, the weights or the slip types changed");
    }
}
