package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.network.PhotoPayloads.RequestPhotoPayload;
import com.dwinovo.chiikawa.network.PhotoPayloads.UploadPhotoPayload;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a player's game says about photos. Nothing it says is believed: a piece is kept only
 * for a photo the server asked that player for, see {@link PhotoSystem}.
 */
public final class PhotoServerPacketHandler {
    private PhotoServerPacketHandler() {
    }

    public static void handleUpload(UploadPhotoPayload payload, ServerPlayer player) {
        PhotoSystem.of(player.getServer()).receive(player, payload.chunk());
    }

    public static void handleRequest(RequestPhotoPayload payload, ServerPlayer player) {
        PhotoSystem.of(player.getServer()).send(player, payload.photo());
    }
}
