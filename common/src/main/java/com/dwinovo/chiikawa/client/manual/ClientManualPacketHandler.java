package com.dwinovo.chiikawa.client.manual;

import com.dwinovo.chiikawa.client.screen.HandbookScreen;
import net.minecraft.client.Minecraft;

/** Opens the handbook when the server says to. */
public final class ClientManualPacketHandler {
    private ClientManualPacketHandler() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new HandbookScreen());
    }
}
