package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class HeartsTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void aRowIsOneHeartForEveryTwoHealth() {
        assertEquals(10, Hearts.count(20));
        assertEquals(5, Hearts.count(9));
    }

    @Test
    void aPetWithNoHealthLeftStillHasItsRow() {
        Hearts.draw(surface, 0, 0, 0, 20);

        assertFalse(surface.hasRectOf(UiTheme.LIFE));
        assertTrue(surface.hasRectOf(UiTheme.SHADE), () -> "the spent hearts left the row: " + surface.rects);
    }

    @Test
    void afullRowIsAllLife() {
        Hearts.draw(surface, 0, 0, 20, 20);

        assertFalse(surface.hasRectOf(UiTheme.SHADE), () -> "a full row had a hole in it: " + surface.rects);
    }

    @Test
    void halfAHeartIsHalfLitAndHalfShaded() {
        Hearts.draw(surface, 0, 0, 1, 2);

        assertTrue(surface.hasRectOf(UiTheme.LIFE));
        assertTrue(surface.hasRectOf(UiTheme.SHADE));
        for (RecordingSurface.Rectangle rect : surface.rects) {
            if (rect.argb() == UiTheme.LIFE) {
                assertTrue(rect.x() + rect.width() <= 3, () -> "the lit half spilled past the middle: " + rect);
            }
        }
    }

    @Test
    void theRowKeepsToTheWidthItSaysItIs() {
        Hearts.draw(surface, 0, 0, 7, 20);

        for (RecordingSurface.Rectangle rect : surface.rects) {
            assertTrue(rect.x() + rect.width() <= Hearts.width(20),
                () -> "a heart spilled out of the row: " + rect);
        }
    }
}
