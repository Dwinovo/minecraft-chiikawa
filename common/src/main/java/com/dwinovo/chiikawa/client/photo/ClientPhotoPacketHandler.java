package com.dwinovo.chiikawa.client.photo;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.client.screen.AlbumScreen;
import com.dwinovo.chiikawa.network.PhotoPayloads.DownloadPhotoPayload;
import com.dwinovo.chiikawa.network.PhotoPayloads.OpenAlbumPayload;
import com.dwinovo.chiikawa.network.PhotoPayloads.TakePhotoPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** What the server says about photos: take one, here is one, look at one. */
public final class ClientPhotoPacketHandler {
    private ClientPhotoPacketHandler() {
    }

    /**
     * Draws the photo the server asked for and sends it back. An entity this game does not
     * know of is not an error: the picture is only ever drawn from what is near the player,
     * and the server lets the request lapse.
     */
    public static void handleTake(TakePhotoPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity viewpoint = minecraft.level == null ? null : minecraft.level.getEntity(payload.viewpoint());
        if (viewpoint == null) {
            Constants.LOG.debug("[chiikawa-photo] No entity {} here to take photo {} from", payload.viewpoint(), payload.photo());
            return;
        }
        PhotoCapture.take(viewpoint, payload.fov())
            .ifPresent(frame -> PhotoUploader.upload(payload.photo(), frame, payload.width(), payload.height()));
    }

    public static void handleDownload(DownloadPhotoPayload payload) {
        ClientPhotoCache.accept(payload.chunk());
    }

    public static void handleOpen(OpenAlbumPayload payload) {
        Minecraft.getInstance().setScreen(new AlbumScreen(payload.shots()));
    }
}
