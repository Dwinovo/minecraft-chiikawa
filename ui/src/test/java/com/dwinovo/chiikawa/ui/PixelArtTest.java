package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PixelArtTest {
    private static final List<PixelArt> SHIPPED = List.of(
        PixelArt.SLIP, PixelArt.SPEECH, PixelArt.PAWS, PixelArt.CUSHION, PixelArt.TUFT, PixelArt.CHECK,
        PixelArt.EMERALD);

    @Test
    void everyIconFitsWhereAnItemWould() {
        for (PixelArt art : SHIPPED) {
            assertTrue(art.width() <= UiStyle.ICON && art.height() <= UiStyle.ICON,
                () -> "an icon bigger than an item: " + art.width() + "x" + art.height());
        }
    }

    @Test
    void everyLetterStandsForAColourOrForNothing() {
        for (PixelArt art : SHIPPED) {
            for (String row : art.rows()) {
                for (char c : row.toCharArray()) {
                    assertTrue(c == '.' || PixelArt.colour(c) != 0, () -> "an unknown letter '" + c + "'");
                }
            }
        }
    }

    @Test
    void aRunOfOneColourIsOneRectangle() {
        RecordingSurface surface = new RecordingSurface();
        new PixelArt("XXXX..ww").draw(surface, 5, 7);

        assertEquals(List.of(
            new RecordingSurface.Rectangle(5, 7, 4, 1, UiTheme.INK),
            new RecordingSurface.Rectangle(11, 7, 2, 1, UiTheme.PANEL)), surface.rects);
    }

    @Test
    void centredMeansCentredInTheBox() {
        RecordingSurface surface = new RecordingSurface();
        new PixelArt("XX", "XX").drawCentered(surface, 0, 0, 16);

        assertEquals(7, surface.rects.get(0).x());
        assertEquals(7, surface.rects.get(0).y());
    }
}
