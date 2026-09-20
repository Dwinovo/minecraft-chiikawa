package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class TooltipTest {
    private final RecordingSurface surface = new RecordingSurface();
    private final List<String> lines = List.of("Weeding", "for a farmer", "12 weeds");

    @Test
    void theCardIsAsWideAsItsLongestLine() {
        assertEquals(surface.textWidth("for a farmer") + 2 * UiStyle.PAD, Tooltip.width(surface, lines));
    }

    @Test
    void theCardIsTallEnoughForEveryLine() {
        assertEquals(2 * UiStyle.PAD + 2 * UiStyle.LINE + surface.lineHeight(), Tooltip.height(surface, lines));
    }

    @Test
    void theCardStaysOnScreenAtTheRightEdge() {
        Rect at = Tooltip.place(surface, lines, 318, 100, 320, 240);

        assertTrue(at.right() <= 320, () -> "ran off the right edge: " + at);
    }

    @Test
    void theCardStaysOnScreenAtTheTop() {
        Rect at = Tooltip.place(surface, lines, 100, 0, 320, 240);

        assertTrue(at.y() >= 0, () -> "ran off the top: " + at);
    }

    @Test
    void theSubjectLeadsAndTheRestSupportIt() {
        Tooltip.draw(surface, lines, 40, 40, 320, 240);

        assertEquals(UiTheme.TEXT, surface.texts.get(0).argb());
        assertEquals(UiTheme.TEXT_MUTED, surface.texts.get(1).argb());
        assertEquals(UiTheme.TEXT_MUTED, surface.texts.get(2).argb());
    }

    @Test
    void nothingToSayDrawsNothing() {
        Tooltip.draw(surface, List.of(), 40, 40, 320, 240);

        assertTrue(surface.rects.isEmpty());
    }
}
