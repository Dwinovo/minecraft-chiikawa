package com.dwinovo.chiikawa.client.music;

import com.dwinovo.chiikawa.client.screen.MusicBoxScreen;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamChunkPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamStartPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicStreamStopPayload;
import net.minecraft.client.Minecraft;

public final class ClientMusicPacketHandler {
    private ClientMusicPacketHandler() {
    }

    public static void handleCatalog(MusicCatalogPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof MusicBoxScreen screen && screen.handIndex() == payload.handIndex()) {
            screen.replaceTracks(payload.tracks());
        } else if (payload.openScreen()) {
            minecraft.setScreenAndShow(new MusicBoxScreen(payload.handIndex(), payload.tracks()));
        }
    }

    public static void handleStreamStart(MusicStreamStartPayload payload) {
        ClientMusicStreamManager.start(payload);
    }

    public static void handleStreamChunk(MusicStreamChunkPayload payload) {
        ClientMusicStreamManager.acceptChunk(payload);
    }

    public static void handleStreamStop(MusicStreamStopPayload payload) {
        ClientMusicStreamManager.stop(payload.sessionId(), payload.reason());
    }
}
