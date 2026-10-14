package com.dwinovo.chiikawa.platform.services;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public interface INetworkHelper {
    void sendToClient(ServerPlayer player, CustomPacketPayload payload);

    void sendToServer(CustomPacketPayload payload);

    /**
     * Opens the pet's screen on the player's game, with the pet's id as the menu's opening data: a whole
     * int, as the horse screen's packet carries the horse's, which a menu data slot (a short) cannot.
     * What a loader opens a menu with data with is its own.
     */
    void openPetMenu(ServerPlayer player, AbstractPet pet);

    /**
     * Whether this player's game takes the payload. A game without the mod does not, nor
     * does the stand-in player an in-game test makes; sending either one a payload it never
     * agreed to is an error on some loaders.
     */
    boolean canReceive(ServerPlayer player, CustomPacketPayload.Type<?> type);

    /**
     * Sends the payload the way the game sends a sound ({@code PlayerList.broadcast}): to
     * every player in the level within {@code range} blocks of {@code pos}, leaving out the
     * games that do not take it.
     */
    default void sendToPlayersNear(ServerLevel level, Vec3 pos, double range, CustomPacketPayload payload) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos) <= range * range && canReceive(player, payload.type())) {
                sendToClient(player, payload);
            }
        }
    }
}
