package com.dwinovo.chiikawa.platform.services;

import com.dwinovo.chiikawa.network.MusicPayloads;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {
    void sendToClient(ServerPlayer player, MusicPayloads.Payload payload);

    void sendToServer(MusicPayloads.Payload payload);

    /**
     * Whether this player's game takes the payload. A game without the mod does not, nor
     * does the stand-in player an in-game test makes; sending either one a payload it never
     * agreed to is an error on some loaders.
     */
    boolean canReceive(ServerPlayer player, ResourceLocation id);
}
