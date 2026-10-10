package com.dwinovo.chiikawa.photo;

import java.io.ByteArrayOutputStream;

/**
 * Puts a photo back together from its {@linkplain PhotoChunk pieces}, which may come in any
 * order, twice, or not at all. It trusts nothing about them: a piece that does not fit what
 * the first one said, or that would take the photo past its size limit, is turned away.
 */
public final class ChunkAssembler {
    /** What became of a piece. */
    public enum Result {
        /** It was kept. */
        ACCEPTED,
        /** It was already here; the first copy stands and the size is not counted twice. */
        DUPLICATE,
        /** It makes no sense: empty, too big, or at odds with the pieces before it. */
        REJECTED,
        /** It would take the photo past its limit. */
        TOO_BIG
    }

    private final int maxBytes;
    private final int maxPieceBytes;
    private final int maxChunks;
    private byte[][] pieces;
    private int received;
    private int size;

    /**
     * @param maxBytes the most the whole may come to
     * @param maxPieceBytes the most one piece may hold
     */
    public ChunkAssembler(int maxBytes, int maxPieceBytes) {
        this.maxBytes = maxBytes;
        this.maxPieceBytes = maxPieceBytes;
        // A sender that fills every piece but the last needs no more than this many.
        this.maxChunks = Math.max(1, (maxBytes + maxPieceBytes - 1) / maxPieceBytes);
    }

    /** An assembler for a photo: no bigger than a photo may be, in pieces of the size {@link PhotoChunk} cuts. */
    public static ChunkAssembler forPhoto() {
        return new ChunkAssembler(PhotoChunk.MAX_PHOTO_BYTES, PhotoChunk.MAX_BYTES);
    }

    public Result add(int index, int total, byte[] data) {
        if (data.length == 0 || data.length > maxPieceBytes || total < 1 || total > maxChunks
                || index < 0 || index >= total || (pieces != null && pieces.length != total)) {
            return Result.REJECTED;
        }
        if (pieces != null && pieces[index] != null) {
            return Result.DUPLICATE;
        }
        if ((long) size + data.length > maxBytes) {
            return Result.TOO_BIG;
        }
        if (pieces == null) {
            pieces = new byte[total][];
        }
        pieces[index] = data.clone();
        received++;
        size += data.length;
        return Result.ACCEPTED;
    }

    public boolean isComplete() {
        return pieces != null && received == pieces.length;
    }

    /** @return the photo, its pieces in order; only once {@link #isComplete} */
    public byte[] assemble() {
        if (!isComplete()) {
            throw new IllegalStateException("pieces are missing: " + received + " of " + (pieces == null ? "?" : pieces.length));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        for (byte[] piece : pieces) {
            out.writeBytes(piece);
        }
        return out.toByteArray();
    }
}
