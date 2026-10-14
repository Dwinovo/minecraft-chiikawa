package com.dwinovo.chiikawa.client.photo;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.network.PhotoPayloads.RequestPhotoPayload;
import com.dwinovo.chiikawa.photo.ChunkAssembler;
import com.dwinovo.chiikawa.photo.PhotoChunk;
import com.dwinovo.chiikawa.platform.Services;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The photos this game has looked at, as textures. Only in memory, and let go of on leaving
 * the world: the server has the photos, and asks nothing of the player's disk. A photo not
 * here is asked of the server the first time it is wanted and arrives in pieces.
 */
public final class ClientPhotoCache {
    /** How far a photo has got. */
    public enum State {
        /** Asked for, not all here yet. */
        LOADING,
        READY,
        /** The server has no such photo, or what it sent was not a picture. */
        MISSING
    }

    /**
     * A photo that is here.
     *
     * @param texture where the game has it
     */
    public record Picture(Identifier texture, int width, int height) {
    }

    private static final class Entry {
        private State state = State.LOADING;
        private final ChunkAssembler pieces = ChunkAssembler.forPhoto();
        private Picture picture;
    }

    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    private ClientPhotoCache() {
    }

    /** Where a photo has got, asking the server for it if this is the first time it is wanted. */
    public static State state(UUID photo) {
        Entry entry = ENTRIES.get(photo);
        if (entry == null) {
            ENTRIES.put(photo, new Entry());
            Services.NETWORK.sendToServer(new RequestPhotoPayload(photo));
            return State.LOADING;
        }
        return entry.state;
    }

    public static Optional<Picture> picture(UUID photo) {
        return Optional.ofNullable(ENTRIES.get(photo)).map(entry -> entry.picture);
    }

    /** Takes in a piece the server sent. One for a photo that was not asked for is dropped. */
    public static void accept(PhotoChunk chunk) {
        Entry entry = ENTRIES.get(chunk.photo());
        if (entry == null || entry.state != State.LOADING) {
            return;
        }
        if (chunk.isMissing()) {
            entry.state = State.MISSING;
            return;
        }
        entry.pieces.add(chunk.index(), chunk.total(), chunk.bytes());
        if (entry.pieces.isComplete()) {
            try {
                // From a stream: reading the bytes themselves takes room on a stack a photo does not fit on.
                NativeImage image = NativeImage.read(new ByteArrayInputStream(entry.pieces.assemble()));
                int width = image.getWidth();
                int height = image.getHeight();
                // The texture owns the image from here, and closes it when it is let go of.
                DynamicTexture texture = new DynamicTexture(() -> "Chiikawa photo " + chunk.photo(), image) {
                    {
                        // Smoothed, because a photo is shown at whatever size the window has room for.
                        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
                    }
                };
                Identifier location = Identifier.fromNamespaceAndPath(Constants.MOD_ID,
                    "photo/" + chunk.photo());
                Minecraft.getInstance().getTextureManager().register(location, texture);
                entry.picture = new Picture(location, width, height);
                entry.state = State.READY;
            } catch (IOException | RuntimeException ex) {
                Constants.LOG.warn("[chiikawa-photo] Photo {} is not a picture", chunk.photo(), ex);
                entry.state = State.MISSING;
            }
        }
    }

    /**
     * Lets go of every photo, on leaving the world. The game only lets go of a texture on its
     * own thread, and Fabric says the world has been left from the connection's.
     */
    public static void clear() {
        Minecraft.getInstance().execute(() -> {
            ENTRIES.values().stream().map(entry -> entry.picture).filter(Objects::nonNull)
                .forEach(picture -> Minecraft.getInstance().getTextureManager().release(picture.texture()));
            ENTRIES.clear();
        });
    }
}
