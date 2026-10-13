package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.network.SettingsPayloads.ChangeWorldSettingsPayload;
import com.dwinovo.chiikawa.network.SettingsPayloads.WorldSettingsPayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.settings.WorldSettings;
import com.dwinovo.chiikawa.settings.WorldSettingsStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps every player's game in step with the world's settings, and takes changes to them.
 * Anything a player's screen says is checked again here: it is the server that holds the
 * settings, and a screen only shows what it was last told.
 */
public final class SettingsServerPacketHandler {
    /** The permission level of a game master: the lowest that may run most commands. */
    private static final int OPERATOR_LEVEL = 2;

    private SettingsServerPacketHandler() {
    }

    /**
     * Whether this player may change the world's settings: an operator, or whoever is playing
     * their own world, who is their own game's master whether or not cheats are on.
     */
    public static boolean canChange(ServerPlayer player) {
        return player.hasPermissions(OPERATOR_LEVEL)
            || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    /** Tells a player what the settings are, and whether they can change them. */
    public static void sync(ServerPlayer player) {
        if (Services.NETWORK.canReceive(player, SettingsPayloads.WORLD_SETTINGS)) {
            Services.NETWORK.sendToClient(player, new WorldSettingsPayload(
                WorldSettingsStore.of(player.server).get(), canChange(player)));
        }
    }

    public static void handleChange(ChangeWorldSettingsPayload payload, ServerPlayer player) {
        MinecraftServer server = player.server;
        if (!canChange(player)) {
            // A screen that offered the change had it wrong; put it right.
            sync(player);
            return;
        }
        WorldSettings settings = payload.settings();
        WorldSettingsStore.of(server).set(settings);
        server.getPlayerList().getPlayers().forEach(SettingsServerPacketHandler::sync);
    }
}
