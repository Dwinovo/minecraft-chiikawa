package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class BannerTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void theBandIsItsColourWithTheInkLineAlongItsBottomRunningOnIntoTheOutline() {
        Banner.draw(surface, 12, 22, 100, 40, UiTheme.ACCENT_PALE);

        assertEquals(UiTheme.ACCENT_PALE, surface.colorAt(12 + 50, 22 + 20));
        assertEquals(UiTheme.INK, surface.colorAt(12 + 50, 22 + 39), "no line along the bottom");
        assertEquals(UiTheme.INK, surface.colorAt(12 - 2, 22 + 39), "the line stops short of the outline");
        assertEquals(UiTheme.INK, surface.colorAt(12 + 101, 22 + 39));
    }

    @Test
    void theTopCornersAreRoundAndTheBottomOnesAreNot() {
        Banner.draw(surface, 12, 22, 100, 40, UiTheme.ACCENT_PALE);

        assertEquals(0, surface.colorAt(12, 22), "the top corner is square");
        assertEquals(UiTheme.ACCENT_PALE, surface.colorAt(12, 22 + 38), "the bottom corner is round");
    }

    @Test
    void theBandIsDottedFaintlyAndNeverOutsideItself() {
        Banner.draw(surface, 12, 22, 100, 40, UiTheme.ACCENT_PALE);

        assertTrue(surface.hasRectOf(UiTheme.DOTS), "no dots");
        for (RecordingSurface.Rectangle rect : surface.rects) {
            if (rect.argb() == UiTheme.DOTS) {
                assertTrue(rect.x() >= 12 && rect.x() < 112 && rect.y() >= 22 && rect.y() < 22 + 39, rect.toString());
            }
        }
    }
}
