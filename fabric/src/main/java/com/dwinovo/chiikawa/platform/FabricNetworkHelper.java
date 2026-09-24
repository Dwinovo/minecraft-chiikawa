package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FabricNetworkHelper implements INetworkHelper {
    @Override
    public void sendToClient(ServerPlayer player, MusicPayloads.Payload payload) {
        FabricModNetworking.sendToClient(player, payload);
    }

    @Override
    public void sendToServer(MusicPayloads.Payload payload) {
        FabricModNetworking.sendToServer(payload);
    }

    @Override
    public boolean canReceive(ServerPlayer player, ResourceLocation id) {
        return ServerPlayNetworking.canSend(player, id);
    }
}
