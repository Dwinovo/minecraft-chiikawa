package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class ChipTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void aChipIsAsWideAsItsWordsAndTheSmallPictureBesideThem() {
        int width = Chip.draw(surface, (s, x, y) -> { }, "Farmer", 10, 20, UiTheme.SURFACE, false);

        assertEquals(Chip.overhead() + surface.textWidth("Farmer"), width);
        assertEquals(width, Chip.width(surface, "Farmer"));
        assertEquals(UiTheme.SURFACE, surface.colorAt(10 + width / 2, 20 + Chip.HEIGHT / 2));
    }

    @Test
    void thePictureAndTheWordsSitInsideTheChipInThatOrder() {
        int[] face = new int[2];
        int width = Chip.draw(surface, (s, x, y) -> {
            face[0] = x;
            face[1] = y;
        }, "Farmer", 10, 20, UiTheme.SURFACE, false);

        RecordingSurface.Text words = surface.texts.get(0);
        assertTrue(face[0] >= 10 + 2, "the picture is on the chip's line");
        assertTrue(words.x() >= face[0] + Chip.FACE, "the words are not beside the picture");
        assertTrue(words.x() + surface.textWidth("Farmer") <= 10 + width - 2, "the words run into the chip's edge");
        assertTrue(face[1] >= 20 && face[1] + Chip.FACE <= 20 + Chip.HEIGHT, "the picture is outside the chip");
    }

    @Test
    void aMarkedChipHasAPinkDotOnItsCornerAndAnotherDoesNot() {
        Chip.draw(surface, (s, x, y) -> { }, "4", 10, 20, UiTheme.SURFACE, true);
        assertTrue(surface.hasRectOf(UiTheme.ACCENT), "no dot on a marked chip");

        RecordingSurface plain = new RecordingSurface();
        Chip.draw(plain, (s, x, y) -> { }, "4", 10, 20, UiTheme.SURFACE, false);
        assertFalse(plain.hasRectOf(UiTheme.ACCENT), "a dot on a chip that is not marked");
    }
}
