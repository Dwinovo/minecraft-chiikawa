package com.dwinovo.chiikawa.platform.services;

import com.dwinovo.chiikawa.network.MusicPayloads;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {
    void sendToClient(ServerPlayer player, MusicPayloads.Payload payload);

    void sendToServer(MusicPayloads.Payload payload);
}
