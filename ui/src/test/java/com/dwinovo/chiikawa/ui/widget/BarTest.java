package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class BarTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void anUntouchedSlipIsAllGroove() {
        Bar.draw(surface, 0, 0, 40, 4, 0, 11);

        assertEquals(1, surface.rects.size());
        assertEquals(UiTheme.SURFACE, surface.rects.get(0).argb());
    }

    @Test
    void aPetWithNoSlipHasNothingToFill() {
        Bar.draw(surface, 0, 0, 40, 4, 3, 0);

        assertFalse(surface.hasRectOf(UiTheme.ACCENT));
    }

    @Test
    void theFirstStepIsVisibleEvenWhenItRoundsToNothing() {
        Bar.draw(surface, 0, 0, 24, 4, 1, 240);

        assertEquals(Bar.MIN_FILL, surface.rectOf(UiTheme.ACCENT).width());
    }

    @Test
    void aBarNearlyThereIsNotDrawnFull() {
        Bar.draw(surface, 0, 0, 24, 4, 239, 240);

        assertTrue(surface.rectOf(UiTheme.ACCENT).width() < 24,
            () -> "nearly done read as done: " + surface.rects);
    }

    @Test
    void aFinishedSlipFillsTheBarAndChangesItsColour() {
        Bar.draw(surface, 0, 0, 24, 4, 11, 11);

        assertEquals(24, surface.rectOf(UiTheme.SUCCESS).width());
        assertFalse(surface.hasRectOf(UiTheme.ACCENT));
    }

    @Test
    void halfDoneIsHalfTheBar() {
        Bar.draw(surface, 0, 0, 40, 4, 5, 10);

        assertEquals(20, surface.rectOf(UiTheme.ACCENT).width());
    }
}
