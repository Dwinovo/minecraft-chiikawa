package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class SliderTest {
    private static final Rect AREA = new Rect(10, 20, 108, 20);
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void theCursorAtEitherEndOfTheTravelPicksTheEnds() {
        // 108 wide with an 8 wide knob: the knob's middle travels from x = 14 to x = 114.
        assertEquals(0.0, Slider.fractionAt(AREA, 14), 1.0E-9);
        assertEquals(1.0, Slider.fractionAt(AREA, 114), 1.0E-9);
        assertEquals(0.5, Slider.fractionAt(AREA, 64), 1.0E-9);
    }

    @Test
    void aCursorPastTheEndsPicksTheEndsAndNothingBeyond() {
        assertEquals(0.0, Slider.fractionAt(AREA, -50), 1.0E-9);
        assertEquals(1.0, Slider.fractionAt(AREA, 500), 1.0E-9);
    }

    @Test
    void theKnobStaysInsideTheGroove() {
        assertEquals(10, Slider.knobX(AREA, 0.0));
        assertEquals(110, Slider.knobX(AREA, 1.0));
        assertEquals(10, Slider.knobX(AREA, -3.0));
        assertEquals(110, Slider.knobX(AREA, 3.0));
    }

    @Test
    void aSliderTooNarrowForItsKnobPicksNothing() {
        assertEquals(0.0, Slider.fractionAt(new Rect(0, 0, Slider.KNOB_W, 20), 4), 1.0E-9);
    }

    @Test
    void theLabelIsWrittenAcrossTheGroove() {
        Slider.draw(surface, AREA, 0.5, "Chatter: 100%", false, true);

        assertEquals(1, surface.texts.size());
        assertEquals("Chatter: 100%", surface.texts.get(0).text());
        assertEquals(UiTheme.TEXT, surface.texts.get(0).argb());
    }

    @Test
    void thePinkFillsTheGrooveAsFarAsTheKnob() {
        Slider.draw(surface, AREA, 0.5, "x", false, true);

        // The knob's left edge is at 60; the fill reaches its middle.
        assertEquals(UiTheme.ACCENT_SOFT, surface.colorAt(30, 30));
        assertEquals(UiTheme.SURFACE, surface.colorAt(100, 30));
    }

    @Test
    void aSliderThatCannotBeMovedIsFlatAndQuiet() {
        Slider.draw(surface, AREA, 0.5, "x", true, false);

        assertFalse(surface.hasRectOf(UiTheme.ACCENT_SOFT));
        assertFalse(surface.hasRectOf(UiTheme.ACCENT));
        assertTrue(surface.hasRectOf(UiTheme.SHADE));
        assertEquals(UiTheme.TEXT_MUTED, surface.texts.get(0).argb());
    }

    @Test
    void theKnobBlushesUnderTheCursor() {
        Slider.draw(surface, AREA, 0.5, "x", true, true);

        assertEquals(UiTheme.ACCENT, surface.colorAt(64, 30));
    }
}
