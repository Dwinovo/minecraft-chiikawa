package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PhotoPngTest {
    private static byte[] png(int width, int height) throws IOException {
        int[] rgb = new int[width * height];
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = 0x336699 + i;
        }
        return PhotoRaster.encodePng(rgb, width, height);
    }

    @Test
    void aPngOfTheAskedForSizeIsAccepted() throws IOException {
        assertTrue(PhotoPng.hasSize(png(32, 24), 32, 24));
    }

    @Test
    void aPngOfAnotherSizeIsNot() throws IOException {
        byte[] png = png(32, 24);

        assertFalse(PhotoPng.hasSize(png, 24, 32));
        assertFalse(PhotoPng.hasSize(png, 33, 24));
        assertFalse(PhotoPng.hasSize(png, 32, 25));
    }

    @Test
    void theSizeIsReadOffTheHeaderBeforeAnythingIsDecoded() throws IOException {
        assertEquals((32L << 32) | 24L, PhotoPng.claimedSize(png(32, 24)).getAsLong());
    }

    @Test
    void whatIsNotAPngIsNot() {
        assertFalse(PhotoPng.hasSize(new byte[0], 32, 24));
        assertFalse(PhotoPng.hasSize("not a picture at all, not even close".getBytes(), 32, 24));
        assertTrue(PhotoPng.claimedSize(new byte[40]).isEmpty());
    }

    @Test
    void aPngCutShortIsNotAcceptedEvenWithAHonestHeader() throws IOException {
        byte[] png = png(32, 24);

        assertFalse(PhotoPng.hasSize(Arrays.copyOf(png, png.length / 2), 32, 24));
    }

    @Test
    void aHeaderThatLiesAboutTheSizeIsFoundOutWithoutDecodingIt() throws IOException {
        byte[] png = png(32, 24);
        // Say it is 100000 wide: the claim alone is enough to turn it away.
        png[16] = 0;
        png[17] = 1;
        png[18] = (byte) 0x86;
        png[19] = (byte) 0xA0;

        assertFalse(PhotoPng.hasSize(png, 32, 24));
        assertFalse(PhotoPng.hasSize(png, 100000, 24));
    }
}
