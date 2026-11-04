package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.data.LaborBoardLevelData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.network.BoardPayloads;
import com.dwinovo.chiikawa.network.BoardServerPacketHandler;
import com.dwinovo.chiikawa.task.BoardLevels;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.task.BoardSlot;
import com.dwinovo.chiikawa.task.PetTaskTypes;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/**
 * What an owner's emeralds buy a labor board. A level is the one thing on that screen an
 * owner can spend money on, so what it costs and what it gives are both worth holding
 * still; what the hunting slips it unlocks then count is in {@code HuntGameTests}.
 */
@GameTestHolder(Constants.MOD_ID)
public final class UpgradeGameTests {
    private static final String BATCH = "chiikawa_upgrade";
    private static final int STAND = 2;
    private static final BlockPos BOARD = new BlockPos(3, STAND, 3);
    /** More than any level costs, so a case about paying is never a case about saving up. */
    private static final int PLENTY = 64;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * The levels come out of the data pack, not the code: with nothing overriding it, what
     * boards go by is exactly what the mod generated, so a pack has one file to replace.
     */
    @GameTest(template = "floor8", batch = BATCH)
    public static void boards_go_by_the_levels_in_the_data_pack(GameTestHelper helper) {
        helper.assertTrue(BoardLevels.current().equals(LaborBoardLevelData.LEVELS),
            Component.literal("boards go by " + BoardLevels.current() + ", not the generated " + LaborBoardLevelData.LEVELS));
        helper.succeed();
    }

    /**
     * The screen is told what the next level gives, since the levels never leave the server:
     * its slips a day, its price, and the work that first goes up at it.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_says_what_its_next_level_gives(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        BoardPayloads.NextLevel next = BoardServerPacketHandler.view(helper.absolutePos(BOARD), board).next();
        BoardLevels levels = BoardLevels.current();
        int level = board.boardLevel();

        helper.assertTrue(next.price() == levels.priceAfter(level), Component.literal("the screen quotes another price: " + next.price()));
        helper.assertTrue(next.daily() == levels.slipsAt(level + 1), Component.literal("the screen promises " + next.daily() + " slips a day"));
        helper.assertTrue(PetTaskTypes.all().entrySet().stream()
                .filter(entry -> entry.getValue().minLevel() == level + 1)
                .allMatch(entry -> next.unlocks().contains(entry.getKey()))
                && !next.unlocks().isEmpty(),
            Component.literal("the screen does not say what work the next level puts up: " + next.unlocks()));
        helper.succeed();
    }

    /** A level buys a slip a day, and leaves the slips already up exactly where they were. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void paying_a_board_up_puts_another_slip_on_it(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        List<BoardSlot> before = List.copyOf(board.today());

        helper.assertTrue(board.upgrade(), Component.literal("a board at its first level had no level to sell"));
        List<BoardSlot> after = board.today();

        helper.assertTrue(after.size() == before.size() + 1,
            Component.literal("a paid-up board puts up " + after.size() + " slips, not one more than " + before.size()));
        helper.assertTrue(after.subList(0, before.size()).equals(before),
            Component.literal("the slips that were already up changed under the pets working on them"));
        helper.succeed();
    }

    /** Emeralds out of the owner's own pockets, and the board a level higher for them. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_takes_the_emeralds_and_goes_up_a_level(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        ServerPlayer owner = customer(helper, PLENTY);
        int price = BoardLevels.current().priceAfter(board.boardLevel());

        helper.assertTrue(BoardServerPacketHandler.buyLevel(helper.absolutePos(BOARD), owner),
            Component.literal("the board would not sell a level to somebody standing at it with the money"));

        helper.assertTrue(board.boardLevel() == BoardLevels.FIRST_LEVEL + 1,
            Component.literal("the board took the money and stayed where it was"));
        helper.assertTrue(owner.getInventory().countItem(Items.EMERALD) == PLENTY - price,
            Component.literal("the board charged something other than the price on its own screen"));
        helper.succeed();
    }

    /** Not enough money is not a discount: the board stays as it is and keeps its hands off. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_nobody_can_pay_for_stays_where_it_is(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        int short_ = BoardLevels.current().priceAfter(board.boardLevel()) - 1;
        ServerPlayer owner = customer(helper, short_);

        helper.assertFalse(BoardServerPacketHandler.buyLevel(helper.absolutePos(BOARD), owner),
            Component.literal("the board sold a level to somebody who was an emerald short"));

        helper.assertTrue(board.boardLevel() == BoardLevels.FIRST_LEVEL, Component.literal("the board went up a level for free"));
        helper.assertTrue(owner.getInventory().countItem(Items.EMERALD) == short_,
            Component.literal("the board took what it was given and gave nothing back"));
        helper.succeed();
    }

    /** And there is a top: past it, there is nothing left to sell and nothing is taken. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_at_the_top_has_nothing_left_to_sell(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        while (board.upgrade()) {
            // up to the top, however many levels that turns out to be
        }
        ServerPlayer owner = customer(helper, PLENTY);

        helper.assertFalse(BoardServerPacketHandler.buyLevel(helper.absolutePos(BOARD), owner),
            Component.literal("a board at its top level sold another one"));

        helper.assertTrue(board.boardLevel() == BoardLevels.current().top(), Component.literal("a board went past its top level"));
        helper.assertTrue(owner.getInventory().countItem(Items.EMERALD) == PLENTY,
            Component.literal("the board charged for a level it did not have"));
        helper.succeed();
    }

    /** A board is bought standing at it, not shouted at from across the field. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_is_not_upgraded_from_across_the_field(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        ServerPlayer owner = customer(helper, PLENTY);
        owner.setPos(helper.absoluteVec(BOARD.getCenter()).add(64.0, 0.0, 0.0));

        helper.assertFalse(BoardServerPacketHandler.buyLevel(helper.absolutePos(BOARD), owner),
            Component.literal("a board sold a level to somebody nowhere near it"));
        helper.assertTrue(board.boardLevel() == BoardLevels.FIRST_LEVEL, Component.literal("the far-off board went up anyway"));
        helper.succeed();
    }

    /**
     * What the levels are for, end to end: a board paid up to where it puts hunting out,
     * and a pet with a sword that walks over and takes one down. Up to here the hunting
     * slips were only ever checked as a rolled list.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 1200)
    public static void a_fencer_takes_a_hunting_slip_off_a_paid_up_board(GameTestHelper helper) {
        BlockPos where = boardOfferingHunting(helper);
        helper.setBlock(where, InitBlocks.LABOR_BOARD.get());
        if (helper.getLevel().getBlockEntity(helper.absolutePos(where)) instanceof LaborBoardBlockEntity board) {
            while (board.upgrade()) {
                // paid up to the top, which is where hunting comes from
            }
        }
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.IRON_SWORD);

        helper.succeedWhen(() -> helper.assertTrue(
            pet.getTask().map(task -> task.counter().equals(PetWorkCounters.SLAY)).orElse(false),
            Component.literal("the fencer came away with " + pet.getTask().map(task -> task.type().toString()).orElse("nothing"))));
    }

    /** A board on the floor, at the level it is placed at. */
    private static LaborBoardBlockEntity board(GameTestHelper helper) {
        helper.setBlock(BOARD, InitBlocks.LABOR_BOARD.get());
        if (helper.getLevel().getBlockEntity(helper.absolutePos(BOARD)) instanceof LaborBoardBlockEntity board) {
            return board;
        }
        throw new AssertionError("the labor board was placed without its block entity");
    }

    /**
     * The first spot on this floor whose top-level roll puts hunting up for a fencer.
     * Which slips a board shows depends on where it stands, so a case that wants a
     * particular kind of work goes looking for a board that has it rather than rolling
     * until it turns up.
     */
    private static BlockPos boardOfferingHunting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long day = level.getDayTime() / Level.TICKS_PER_DAY;
        ResourceLocation fencer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FENCER.get());
        for (int x = 8; x < 15; x++) {
            for (int z = 2; z < 15; z++) {
                BlockPos rel = new BlockPos(x, STAND, z);
                long seed = BoardSlips.seed(level.getSeed(), day, helper.absolutePos(rel));
                boolean hunting = BoardSlips.roll(seed, PetTaskTypes.all(), BoardLevels.current(), BoardLevels.current().top()).stream()
                    .anyMatch(slot -> slot.slip().capability().equals(fencer));
                if (hunting) {
                    return rel;
                }
            }
        }
        throw new AssertionError("nowhere on this floor puts hunting up today — "
            + "the roll, the weights or the slip types changed");
    }

    /** Somebody at the board with emeralds in their pockets. */
    private static ServerPlayer customer(GameTestHelper helper, int emeralds) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        owner.setPos(helper.absoluteVec(BOARD.getCenter()));
        if (emeralds > 0) {
            owner.getInventory().add(new ItemStack(Items.EMERALD, emeralds));
        }
        return owner;
    }
}
