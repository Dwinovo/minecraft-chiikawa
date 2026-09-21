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
        Bar.draw(surface, 0, 0, 40, 7, 0, 11);

        assertFalse(surface.hasRectOf(UiTheme.LEAF));
        assertEquals(UiTheme.SURFACE, surface.colorAt(20, 3));
        assertEquals(UiTheme.INK, surface.colorAt(20, 0), "the track has no line round it");
    }

    @Test
    void aPetWithNoSlipHasNothingToFill() {
        Bar.draw(surface, 0, 0, 40, 7, 3, 0);

        assertFalse(surface.hasRectOf(UiTheme.LEAF));
    }

    @Test
    void theFirstStepIsVisibleEvenWhenItRoundsToNothing() {
        Bar.draw(surface, 0, 0, 24, 7, 1, 240);

        assertEquals(UiTheme.LEAF, surface.colorAt(1, 3), "the first step is not there to see");
    }

    @Test
    void aBarNearlyThereIsNotDrawnFull() {
        Bar.draw(surface, 0, 0, 24, 7, 239, 240);

        assertEquals(UiTheme.SURFACE, surface.colorAt(22, 3), () -> "nearly done read as done: " + surface.rects);
        assertFalse(surface.hasRectOf(UiTheme.SUCCESS));
    }

    @Test
    void aFinishedSlipFillsTheBarAndChangesItsColour() {
        Bar.draw(surface, 0, 0, 24, 7, 11, 11);

        assertEquals(UiTheme.SUCCESS, surface.colorAt(22, 3));
        assertEquals(UiTheme.SUCCESS, surface.colorAt(1, 3));
        assertFalse(surface.hasRectOf(UiTheme.LEAF));
    }

    @Test
    void halfDoneIsHalfTheBar() {
        Bar.draw(surface, 0, 0, 42, 7, 5, 10);

        // Forty pixels of track inside the line: half of it is twenty.
        assertEquals(UiTheme.LEAF, surface.colorAt(20, 3));
        assertEquals(UiTheme.SURFACE, surface.colorAt(21, 3));
    }
}
