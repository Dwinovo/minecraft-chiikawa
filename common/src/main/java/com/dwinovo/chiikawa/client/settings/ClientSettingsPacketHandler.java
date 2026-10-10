package com.dwinovo.chiikawa.client.settings;

import com.dwinovo.chiikawa.network.SettingsPayloads.WorldSettingsPayload;

/** Takes in what the server says the world's settings are. */
public final class ClientSettingsPacketHandler {
    private ClientSettingsPacketHandler() {
    }

    public static void handleWorld(WorldSettingsPayload payload) {
        ClientWorldSettings.accept(new ClientWorldSettings.Synced(payload.settings(), payload.canChange()));
    }
}
