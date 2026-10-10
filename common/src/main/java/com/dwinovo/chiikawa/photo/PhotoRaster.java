package com.dwinovo.chiikawa.photo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Turning what the game drew into the picture a photo is: the middle of it, cut to the
 * photo's shape, shrunk to its size, and written as a PNG. Plain pixels in and out, so it
 * can be done on any thread and tried without a game.
 */
public final class PhotoRaster {
    private PhotoRaster() {
    }

    /**
     * Cuts the largest piece of the picture that has the shape {@code width} by
     * {@code height}, from the middle, and scales it to that size by averaging the pixels
     * that fall in each new one. A picture smaller than the photo is enlarged without
     * smoothing.
     *
     * @param abgr the picture's pixels as the game keeps them, one int each, alpha highest,
     *             then blue, green and red, row by row from the top
     * @return opaque pixels, one int each in the order {@link BufferedImage#TYPE_INT_RGB} wants
     */
    public static int[] cropAndScale(int[] abgr, int sourceWidth, int sourceHeight, int width, int height) {
        // The piece of the picture to keep: as wide as it can be at the photo's shape.
        int cropWidth = sourceWidth;
        int cropHeight = sourceHeight;
        if ((long) sourceWidth * height > (long) sourceHeight * width) {
            cropWidth = Math.max(1, (int) Math.round((double) sourceHeight * width / height));
        } else {
            cropHeight = Math.max(1, (int) Math.round((double) sourceWidth * height / width));
        }
        int cropX = (sourceWidth - cropWidth) / 2;
        int cropY = (sourceHeight - cropHeight) / 2;

        int[] out = new int[width * height];
        for (int y = 0; y < height; y++) {
            int fromY = cropY + (int) ((long) y * cropHeight / height);
            int toY = Math.max(fromY + 1, cropY + (int) ((long) (y + 1) * cropHeight / height));
            for (int x = 0; x < width; x++) {
                int fromX = cropX + (int) ((long) x * cropWidth / width);
                int toX = Math.max(fromX + 1, cropX + (int) ((long) (x + 1) * cropWidth / width));
                long red = 0;
                long green = 0;
                long blue = 0;
                for (int sy = fromY; sy < toY; sy++) {
                    for (int sx = fromX; sx < toX; sx++) {
                        int pixel = abgr[sy * sourceWidth + sx];
                        red += pixel & 0xFF;
                        green += (pixel >> 8) & 0xFF;
                        blue += (pixel >> 16) & 0xFF;
                    }
                }
                long count = (long) (toY - fromY) * (toX - fromX);
                out[y * width + x] = ((int) (red / count) << 16) | ((int) (green / count) << 8) | (int) (blue / count);
            }
        }
        return out;
    }

    /** @param rgb opaque pixels as {@link #cropAndScale} makes them */
    public static byte[] encodePng(int[] rgb, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, rgb, 0, width);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("no PNG writer");
        }
        return out.toByteArray();
    }
}
