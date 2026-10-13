package com.dwinovo.chiikawa.photo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One piece of a photo's PNG on its way between the player's game and the server. A packet
 * from the game to the server holds 32 KiB at most, and a photo is more than that, so it
 * goes in pieces, each saying which photo it is of, where it goes and how many there are.
 *
 * @param photo which photo
 * @param index which piece, counting from 0
 * @param total how many pieces there are in all; 0 only in {@link #missing}
 * @param bytes the piece itself
 */
public record PhotoChunk(UUID photo, int index, int total, byte[] bytes) {
    /** The most a piece holds, comfortably under what one packet can carry. */
    public static final int MAX_BYTES = 30_000;
    /** The most a whole photo may come to, 512 KiB: a 320 by 240 picture is a fraction of it. */
    public static final int MAX_PHOTO_BYTES = 512 * 1024;

    public static PhotoChunk read(FriendlyByteBuf buffer) {
        return new PhotoChunk(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(MAX_BYTES));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(photo);
        buffer.writeVarInt(index);
        buffer.writeVarInt(total);
        buffer.writeByteArray(bytes);
    }

    /**
     * An answer to a request for a photo the server has none of: no pieces at all, which
     * cannot be mistaken for the first of some.
     */
    public static PhotoChunk missing(UUID photo) {
        return new PhotoChunk(photo, 0, 0, new byte[0]);
    }

    public boolean isMissing() {
        return total == 0;
    }

    /** A photo cut into pieces of at most {@link #MAX_BYTES}, in order. */
    public static List<PhotoChunk> split(UUID photo, byte[] png) {
        int total = Math.max(1, (png.length + MAX_BYTES - 1) / MAX_BYTES);
        List<PhotoChunk> chunks = new ArrayList<>(total);
        for (int index = 0; index < total; index++) {
            chunks.add(new PhotoChunk(photo, index, total,
                Arrays.copyOfRange(png, index * MAX_BYTES, Math.min(png.length, (index + 1) * MAX_BYTES))));
        }
        return chunks;
    }
}
