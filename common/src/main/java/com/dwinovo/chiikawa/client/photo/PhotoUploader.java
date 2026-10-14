package com.dwinovo.chiikawa.client.photo;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.network.PhotoPayloads.UploadPhotoPayload;
import com.dwinovo.chiikawa.photo.PhotoChunk;
import com.dwinovo.chiikawa.photo.PhotoRaster;
import com.dwinovo.chiikawa.platform.Services;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/**
 * Turns a frame into a photo and sends it to the server. Cutting, shrinking and compressing
 * a window-sized picture takes long enough to be felt, so it is done away from the game's
 * own thread; only the sending comes back to it.
 */
public final class PhotoUploader {
    private PhotoUploader() {
    }

    /**
     * @param photo the id the server asked the picture for
     * @param frame what was drawn
     * @param width how wide the photo is to be
     * @param height how tall
     */
    public static void upload(UUID photo, PhotoCapture.Frame frame, int width, int height) {
        CompletableFuture.supplyAsync(() -> {
            int[] rgb = PhotoRaster.cropAndScale(frame.abgr(), frame.width(), frame.height(), width, height);
            try {
                return PhotoRaster.encodePng(rgb, width, height);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }, Util.backgroundExecutor()).whenComplete((png, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                Constants.LOG.error("[chiikawa-photo] Could not make photo {}", photo, error);
            } else if (png.length > PhotoChunk.MAX_PHOTO_BYTES) {
                Constants.LOG.error("[chiikawa-photo] Photo {} came to {} bytes, more than a photo may", photo, png.length);
            } else {
                PhotoChunk.split(photo, png).forEach(chunk -> Services.NETWORK.sendToServer(new UploadPhotoPayload(chunk)));
            }
        }));
    }
}
