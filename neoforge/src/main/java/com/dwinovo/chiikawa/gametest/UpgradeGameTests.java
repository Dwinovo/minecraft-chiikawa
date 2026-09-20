package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.network.BoardServerPacketHandler;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.task.BoardSlot;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What an owner's emeralds buy a labor board. A level is the one thing on that screen an
 * owner can spend money on, so what it costs and what it gives are both worth holding
 * still; what the hunting slips it unlocks then count is in {@code HuntGameTests}.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
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

    /** A level buys a slip a day, and leaves the slips already up exactly where they were. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void paying_a_board_up_puts_another_slip_on_it(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        List<BoardSlot> before = List.copyOf(board.today());

        helper.assertTrue(board.upgrade(), "a board at its first level had no level to sell");
        List<BoardSlot> after = board.today();

        helper.assertTrue(after.size() == before.size() + 1,
            "a paid-up board puts up " + after.size() + " slips, not one more than " + before.size());
        helper.assertTrue(after.subList(0, before.size()).equals(before),
            "the slips that were already up changed under the pets working on them");
        helper.succeed();
    }

    /** Emeralds out of the owner's own pockets, and the board a level higher for them. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_takes_the_emeralds_and_goes_up_a_level(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        ServerPlayer owner = customer(helper, PLENTY);
        int price = BoardSlips.upgradePrice(board.boardLevel());

        helper.assertTrue(BoardServerPacketHandler.buyLevel(helper.absolutePos(BOARD), owner),
            "the board would not sell a level to somebody standing at it with the money");

        helper.assertTrue(board.boardLevel() == BoardSlips.FIRST_LEVEL + 1,
            "the board took the money and stayed where it was");
        helper.assertTrue(owner.getInventory().countItem(Items.EMERALD) == PLENTY - price,
            "the board charged something other than the price on its own screen");
        helper.succeed();
    }

    /** Not enough money is not a discount: the board stays as it is and keeps its hands off. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_nobody_can_pay_for_stays_where_it_is(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        int short_ = BoardSlips.upgradePrice(board.boardLevel()) - 1;
        ServerPlayer owner = customer(helper, short_);

        helper.assertFalse(BoardServerPacketHandler.buyLevel(helper.absolutePos(BOARD), owner),
            "the board sold a level to somebody who was an emerald short");

        helper.assertTrue(board.boardLevel() == BoardSlips.FIRST_LEVEL, "the board went up a level for free");
        helper.assertTrue(owner.getInventory().countItem(Items.EMERALD) == short_,
            "the board took what it was given and gave nothing back");
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
            "a board at its top level sold another one");

        helper.assertTrue(board.boardLevel() == BoardSlips.MAX_LEVEL, "a board went past its top level");
        helper.assertTrue(owner.getInventory().countItem(Items.EMERALD) == PLENTY,
            "the board charged for a level it did not have");
        helper.succeed();
    }

    /** A board is bought standing at it, not shouted at from across the field. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_board_is_not_upgraded_from_across_the_field(GameTestHelper helper) {
        LaborBoardBlockEntity board = board(helper);
        ServerPlayer owner = customer(helper, PLENTY);
        owner.setPos(helper.absoluteVec(BOARD.getCenter()).add(64.0, 0.0, 0.0));

        helper.assertFalse(BoardServerPacketHandler.buyLevel(helper.absolutePos(BOARD), owner),
            "a board sold a level to somebody nowhere near it");
        helper.assertTrue(board.boardLevel() == BoardSlips.FIRST_LEVEL, "the far-off board went up anyway");
        helper.succeed();
    }

    /** A board on the floor, at the level it is placed at. */
    private static LaborBoardBlockEntity board(GameTestHelper helper) {
        helper.setBlock(BOARD, InitBlocks.LABOR_BOARD.get());
        if (helper.getBlockEntity(BOARD) instanceof LaborBoardBlockEntity board) {
            return board;
        }
        throw new AssertionError("the labor board was placed without its block entity");
    }

    /** Somebody at the board with emeralds in their pockets. */
    private static ServerPlayer customer(GameTestHelper helper, int emeralds) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(GameType.SURVIVAL);
        owner.setPos(helper.absoluteVec(BOARD.getCenter()));
        if (emeralds > 0) {
            owner.getInventory().add(new ItemStack(Items.EMERALD, emeralds));
        }
        return owner;
    }
}
