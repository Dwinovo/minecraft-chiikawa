package com.dwinovo.chiikawa.photo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.OptionalLong;
import javax.imageio.ImageIO;

/**
 * Telling whether bytes a player's game sent are a PNG of the size that was asked for,
 * before anything is kept. The size is read off the file's header first, so a file that
 * claims to be enormous is turned away without being decoded.
 */
public final class PhotoPng {
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    /** The signature, then the first chunk's length and name, then its width and height. */
    private static final int HEADER_BYTES = 24;
    private static final int IHDR = 0x49484452;
    private static final int IHDR_AT = 12;
    private static final int WIDTH_AT = 16;
    private static final int HEIGHT_AT = 20;

    private PhotoPng() {
    }

    /**
     * @return the size the header of this PNG claims, packed as width in the high half and
     *         height in the low, or nothing when it does not start like a PNG
     */
    static OptionalLong claimedSize(byte[] png) {
        if (png.length < HEADER_BYTES) {
            return OptionalLong.empty();
        }
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (png[i] != SIGNATURE[i]) {
                return OptionalLong.empty();
            }
        }
        ByteBuffer header = ByteBuffer.wrap(png, 0, HEADER_BYTES);
        if (header.getInt(IHDR_AT) != IHDR) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(((long) header.getInt(WIDTH_AT) << 32) | (header.getInt(HEIGHT_AT) & 0xFFFFFFFFL));
    }

    /** Whether this is a PNG, whole and readable, exactly {@code width} by {@code height}. */
    public static boolean hasSize(byte[] png, int width, int height) {
        OptionalLong claimed = claimedSize(png);
        if (claimed.isEmpty() || claimed.getAsLong() != (((long) width << 32) | height)) {
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            return image != null && image.getWidth() == width && image.getHeight() == height;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }
}
