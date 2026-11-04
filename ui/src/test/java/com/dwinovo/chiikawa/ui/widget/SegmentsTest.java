package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class SegmentsTest {
    private static final List<String> LABELS = List.of("Follow", "Sit", "Free Roam");
    private static final Rect AREA = new Rect(10, 20, 110, Segments.HEIGHT);
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void theCellsFillTheControlEdgeToEdgeWithALineBetweenEachTwo() {
        List<Rect> cells = Segments.cells(surface::textWidth, LABELS, AREA);

        assertEquals(AREA.x() + 1, cells.get(0).x());
        for (int index = 1; index < cells.size(); index++) {
            assertEquals(cells.get(index - 1).right() + 1, cells.get(index).x(), "the cells do not meet at a line");
        }
        assertEquals(AREA.right() - 1, cells.get(2).right(), "the last cell stops short of the sticker's line");
    }

    @Test
    void aLongerWordGetsAWiderCellAndTheRoomLeftOverIsShared() {
        List<Rect> cells = Segments.cells(surface::textWidth, LABELS, AREA);

        assertTrue(cells.get(2).width() > cells.get(1).width(), "a long word has no more room than a short one");
        int slackShare = cells.get(1).width() - surface.textWidth("Sit") - 2 * Segments.PAD;
        assertTrue(Math.abs(cells.get(0).width() - surface.textWidth("Follow") - 2 * Segments.PAD - slackShare) <= 1,
            "the extra room is not shared evenly");
    }

    @Test
    void theControlNeedsAsMuchRoomAsItsWordsAndTheirAir() {
        int words = 0;
        for (String label : LABELS) {
            words += surface.textWidth(label);
        }

        assertEquals(words + 2 * Segments.PAD * LABELS.size() + LABELS.size() - 1 + 2, Segments.minWidth(surface::textWidth, LABELS));
    }

    @Test
    void theOneInForceIsPinkAndTheOthersArePaper() {
        Segments.draw(surface, AREA, LABELS, 1, -1);
        List<Rect> cells = Segments.cells(surface::textWidth, LABELS, AREA);

        assertEquals(UiTheme.ACCENT, surface.colorAt(cells.get(1).x() + 2, AREA.y() + 3));
        assertEquals(UiTheme.PANEL, surface.colorAt(cells.get(0).x() + 2, AREA.y() + 3));
        assertEquals(UiTheme.PANEL, surface.colorAt(cells.get(2).x() + 2, AREA.y() + 3));
        assertEquals(UiTheme.INK, surface.colorAt(cells.get(0).right(), AREA.y() + 5), "no line between the cells");
    }

    @Test
    void theCursorBlushesACellThatIsNotInForceAndLeavesTheOneThatIs() {
        Segments.draw(surface, AREA, LABELS, 0, 2);
        List<Rect> cells = Segments.cells(surface::textWidth, LABELS, AREA);

        assertEquals(UiTheme.ACCENT_PALE, surface.colorAt(cells.get(2).x() + 2, AREA.y() + 3));
        assertEquals(UiTheme.ACCENT, surface.colorAt(cells.get(0).x() + 2, AREA.y() + 3));
    }

    @Test
    void theWordsAreCentredInTheirCellsAndThePickedOneIsInTheInk() {
        Segments.draw(surface, AREA, LABELS, 0, -1);
        List<Rect> cells = Segments.cells(surface::textWidth, LABELS, AREA);

        assertEquals(3, surface.texts.size());
        RecordingSurface.Text first = surface.texts.get(0);
        assertEquals(cells.get(0).x() + (cells.get(0).width() - surface.textWidth("Follow")) / 2, first.x());
        assertEquals(UiTheme.TEXT, first.argb());
        assertEquals(UiTheme.TEXT_MUTED, surface.texts.get(1).argb());
    }
}
