package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.platform.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeNetworkHelper implements INetworkHelper {
    @Override
    public void sendToClient(ServerPlayer player, MusicPayloads.Payload payload) {
        ForgeModNetworking.sendToClient(player, payload);
    }

    @Override
    public void sendToServer(MusicPayloads.Payload payload) {
        ForgeModNetworking.sendToServer(payload);
    }
}
