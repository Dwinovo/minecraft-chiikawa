package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.task.BoardSlips;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Levels bought for a labor board. The screen shows a price and the server charges it:
 * a screen is a picture of what the board was a moment ago, and the emeralds leave the
 * player's pockets here.
 */
public final class BoardServerPacketHandler {
    /** How far from a board an owner may stand and still buy it a level. */
    private static final double REACH_SQR = 64.0;

    private BoardServerPacketHandler() {
    }

    /** @return the board as its screen shows it */
    public static BoardPayloads.BoardSlipsPayload view(BlockPos pos, LaborBoardBlockEntity board) {
        return new BoardPayloads.BoardSlipsPayload(pos, board.boardLevel(),
            BoardSlips.upgradePrice(board.boardLevel()), board.slipViews());
    }

    public static void handleUpgrade(BoardPayloads.BoardUpgradePayload payload, ServerPlayer player) {
        buyLevel(payload.board(), player);
        // Sold or not, the screen is sent back as the board now stands, so an owner who
        // could not pay sees why rather than clicking at a button that does nothing.
        player.level().getBlockEntity(payload.board(), InitBlockEntities.LABOR_BOARD.get())
            .ifPresent(board -> Services.NETWORK.sendToClient(player, view(payload.board(), board)));
    }

    /**
     * Sells a board its next level, if the owner is standing at it and has the emeralds.
     *
     * @return whether the board went up a level
     */
    public static boolean buyLevel(BlockPos pos, ServerPlayer player) {
        if (player.distanceToSqr(pos.getCenter()) > REACH_SQR) {
            return false;
        }
        Optional<LaborBoardBlockEntity> found = player.level()
            .getBlockEntity(pos, InitBlockEntities.LABOR_BOARD.get());
        if (found.isEmpty()) {
            return false;
        }
        LaborBoardBlockEntity board = found.get();
        int price = BoardSlips.upgradePrice(board.boardLevel());
        // A board at its top level has nothing to sell, and a price is paid in full or not
        // at all: nobody leaves half the emeralds on the counter for half a level.
        if (price <= 0 || !Wallet.pay(player.getInventory(), price)) {
            return false;
        }
        board.upgrade();
        player.level().playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.4F);
        player.containerMenu.broadcastChanges();
        return true;
    }
}
