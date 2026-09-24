package com.dwinovo.chiikawa.client.board;

import com.dwinovo.chiikawa.client.screen.LaborBoardScreen;
import com.dwinovo.chiikawa.network.BoardPayloads.BoardSlipsPayload;
import net.minecraft.client.Minecraft;

/** Opens the labor board screen with the slips the server sent. */
public final class ClientBoardPacketHandler {
    private ClientBoardPacketHandler() {
    }

    public static void handleSlips(BoardSlipsPayload payload) {
        Minecraft.getInstance().setScreenAndShow(new LaborBoardScreen(payload));
    }
}
