package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class SwitchTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void onIsPinkWithTheKnobAtTheRight() {
        Switch.draw(surface, 10, 20, true, false);

        assertEquals(UiTheme.ACCENT, surface.colorAt(10 + 4, 20 + Switch.HEIGHT / 2), "the pill is not pink when on");
        assertEquals(UiTheme.HIGHLIGHT, surface.colorAt(10 + Switch.WIDTH - 7, 20 + Switch.HEIGHT / 2), "no knob at the right");
    }

    @Test
    void offIsPaperWithTheKnobAtTheLeft() {
        Switch.draw(surface, 10, 20, false, false);

        assertEquals(UiTheme.SURFACE, surface.colorAt(10 + Switch.WIDTH - 5, 20 + Switch.HEIGHT / 2), "the pill is not paper when off");
        assertEquals(UiTheme.HIGHLIGHT, surface.colorAt(10 + 6, 20 + Switch.HEIGHT / 2), "no knob at the left");
    }

    @Test
    void aSwitchThatIsOffBlushesUnderTheCursor() {
        Switch.draw(surface, 10, 20, false, true);

        assertEquals(UiTheme.ACCENT_PALE, surface.colorAt(10 + Switch.WIDTH - 5, 20 + Switch.HEIGHT / 2));
    }

    @Test
    void theOutlineIsTheInkAllRoundTheMiddle() {
        Switch.draw(surface, 10, 20, true, false);

        assertEquals(UiTheme.INK, surface.colorAt(10 + Switch.WIDTH / 2, 20));
        assertEquals(UiTheme.INK, surface.colorAt(10 + Switch.WIDTH / 2, 20 + Switch.HEIGHT - 1));
    }
}
