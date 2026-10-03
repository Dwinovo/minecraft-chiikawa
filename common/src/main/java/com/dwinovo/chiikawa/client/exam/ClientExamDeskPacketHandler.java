package com.dwinovo.chiikawa.client.exam;

import com.dwinovo.chiikawa.client.screen.ExamDeskScreen;
import com.dwinovo.chiikawa.network.ExamDeskPayloads.DeskViewPayload;
import net.minecraft.client.Minecraft;

/** Opens the exam desk screen with what the server sent. */
public final class ClientExamDeskPacketHandler {
    private ClientExamDeskPacketHandler() {
    }

    public static void handleView(DeskViewPayload payload) {
        Minecraft.getInstance().setScreen(new ExamDeskScreen(payload));
    }
}
