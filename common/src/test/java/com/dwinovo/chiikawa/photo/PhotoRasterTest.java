package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PhotoRasterTest {
    /** One pixel in the game's order: alpha, blue, green, red. */
    private static int abgr(int red, int green, int blue) {
        return 0xFF000000 | (blue << 16) | (green << 8) | red;
    }

    private static int[] fill(int width, int height, int pixel) {
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, pixel);
        return pixels;
    }

    @Test
    void colourComesOutInTheOrderAPngWantsAndOpaque() {
        int[] out = PhotoRaster.cropAndScale(fill(4, 3, abgr(0x11, 0x22, 0x33)), 4, 3, 4, 3);

        assertEquals(0x112233, out[0]);
        assertEquals(12, out.length);
    }

    @Test
    void aPictureWiderThanThePhotoIsCutFromTheMiddle() {
        // 8 by 3 into 4 by 3: the two columns at either side go.
        int[] source = new int[8 * 3];
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 8; x++) {
                source[y * 8 + x] = abgr(x * 10, 0, 0);
            }
        }

        int[] out = PhotoRaster.cropAndScale(source, 8, 3, 4, 3);

        assertEquals(20 << 16, out[0], "the first column kept is the third");
        assertEquals(50 << 16, out[3], "the last is the sixth");
    }

    @Test
    void aPictureTallerThanThePhotoIsCutFromTheMiddleToo() {
        // 4 by 9 into 4 by 3: three rows at the top and three at the bottom go.
        int[] source = new int[4 * 9];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 4; x++) {
                source[y * 4 + x] = abgr(0, y * 10, 0);
            }
        }

        int[] out = PhotoRaster.cropAndScale(source, 4, 9, 4, 3);

        assertEquals(30 << 8, out[0]);
        assertEquals(50 << 8, out[2 * 4]);
    }

    @Test
    void shrinkingAveragesThePixelsThatFallInEachNewOne() {
        // 4 by 2 into 2 by 1: each new pixel is the mean of a 2 by 2 block.
        int[] source = {
            abgr(0, 0, 0), abgr(100, 0, 0), abgr(200, 0, 0), abgr(200, 0, 0),
            abgr(100, 0, 0), abgr(0, 0, 0), abgr(200, 0, 0), abgr(200, 0, 0)};

        int[] out = PhotoRaster.cropAndScale(source, 4, 2, 2, 1);

        assertEquals(50 << 16, out[0]);
        assertEquals(200 << 16, out[1]);
    }

    @Test
    void aPictureSmallerThanThePhotoIsEnlargedWithoutSmoothing() {
        int[] source = {abgr(10, 0, 0), abgr(20, 0, 0)};

        int[] out = PhotoRaster.cropAndScale(source, 2, 1, 4, 2);

        assertEquals(10 << 16, out[0]);
        assertEquals(10 << 16, out[1]);
        assertEquals(20 << 16, out[2]);
        assertEquals(20 << 16, out[3]);
        assertEquals(out[0], out[4]);
    }

    @Test
    void aWindowOfAnyShapeComesOutTheSizeOfThePhoto() {
        for (int[] size : new int[][] {{1920, 1080}, {854, 480}, {300, 900}, {17, 5}}) {
            int[] out = PhotoRaster.cropAndScale(fill(size[0], size[1], abgr(1, 2, 3)), size[0], size[1], 32, 24);

            assertEquals(32 * 24, out.length);
            assertEquals(0x010203, out[out.length - 1]);
        }
    }

    @Test
    void whatIsEncodedIsAPngOfTheSameSizeAndPixels() throws IOException {
        int[] rgb = new int[6 * 4];
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = i * 0x0A0B0C & 0xFFFFFF;
        }

        BufferedImage read = ImageIO.read(new ByteArrayInputStream(PhotoRaster.encodePng(rgb, 6, 4)));

        assertEquals(6, read.getWidth());
        assertEquals(4, read.getHeight());
        assertEquals(rgb[7], read.getRGB(1, 1) & 0xFFFFFF);
    }
}
