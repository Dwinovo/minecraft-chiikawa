package com.dwinovo.chiikawa.photo;

import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.network.PhotoPayloads.DownloadPhotoPayload;
import com.dwinovo.chiikawa.network.PhotoPayloads.TakePhotoPayload;
import com.dwinovo.chiikawa.platform.Services;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The server's photos: asking a player's game for one, taking in what it sends back, and
 * handing out the ones in the library. One per running server, made the first time anything
 * asks.
 *
 * <p>Who asks for a photo says what to do with it: it is told when the photo is in the
 * library, or when it is not coming, and a photo that never arrives costs nothing.
 */
public final class PhotoSystem {
    private static final Map<MinecraftServer, PhotoSystem> SYSTEMS = new WeakHashMap<>();
    /** Where the photos of a world are kept, inside the world's own folder. */
    private static final String FOLDER = "data/chiikawa/photos";

    private final MinecraftServer server;
    private final PhotoLibrary library;
    private final PhotoUploads uploads = new PhotoUploads();

    private PhotoSystem(MinecraftServer server) {
        this.server = server;
        this.library = new PhotoLibrary(server.getWorldPath(LevelResource.ROOT).resolve(FOLDER));
    }

    public static PhotoSystem of(MinecraftServer server) {
        return SYSTEMS.computeIfAbsent(server, PhotoSystem::new);
    }

    public static void tickServer(MinecraftServer server) {
        of(server).uploads.expire(server.getTickCount());
    }

    public static void stopServer(MinecraftServer server) {
        SYSTEMS.remove(server);
    }

    public PhotoLibrary library() {
        return library;
    }

    /**
     * Asks the owner's game to draw a picture from an entity's eyes and send it back. The
     * request stands for a minute whether or not the game takes it up.
     *
     * @param owner whose game draws it, and the only one whose picture is taken
     * @param viewpoint whose eyes it is drawn from; the owner's game has to know of it
     * @param camera how big the picture is and how wide the lens sees
     * @param listener told how it turned out
     * @return the id the picture is kept under, if it comes
     */
    public UUID request(ServerPlayer owner, Entity viewpoint, CameraSettings camera, PhotoUploads.Listener listener) {
        UUID photo = UUID.randomUUID();
        uploads.expect(new PhotoUploads.Request(photo, owner.getUUID(), camera.width(), camera.height()),
            server.getTickCount(), listener);
        // A game that cannot take the request is left to let it lapse, as one that is slow is.
        if (Services.NETWORK.canReceive(owner, TakePhotoPayload.TYPE)) {
            Services.NETWORK.sendToClient(owner,
                new TakePhotoPayload(photo, viewpoint.getId(), camera.width(), camera.height(), camera.fov()));
        }
        return photo;
    }

    /**
     * Takes in a piece of a photo a game sent. Anything the server did not ask for is dropped;
     * the photo is kept once all of it is here and it turns out to be the size that was asked for.
     */
    public void receive(ServerPlayer sender, PhotoChunk chunk) {
        uploads.accept(chunk.photo(), sender.getUUID(), chunk.index(), chunk.total(), chunk.bytes(),
            server.getTickCount()).ifPresent(arrived -> {
                PhotoUploads.Request request = arrived.request();
                boolean kept = PhotoPng.hasSize(arrived.png(), request.width(), request.height())
                    && library.save(request.photo(), arrived.png());
                uploads.finish(request.photo(), kept);
            });
    }

    /** Sends a player a photo from the library, piece by piece, or the word that there is none. */
    public void send(ServerPlayer player, UUID photo) {
        if (!Services.NETWORK.canReceive(player, DownloadPhotoPayload.TYPE)) {
            return;
        }
        Optional<byte[]> png = library.read(photo);
        if (png.isEmpty()) {
            Services.NETWORK.sendToClient(player, new DownloadPhotoPayload(PhotoChunk.missing(photo)));
            return;
        }
        for (PhotoChunk chunk : PhotoChunk.split(photo, png.get())) {
            Services.NETWORK.sendToClient(player, new DownloadPhotoPayload(chunk));
        }
    }
}
