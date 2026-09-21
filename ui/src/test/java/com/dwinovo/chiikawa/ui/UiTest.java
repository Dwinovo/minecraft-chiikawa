package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.RecordingSurface.Rectangle;
import com.dwinovo.chiikawa.ui.RecordingSurface.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void aPanelIsRoundedAtEveryCorner() {
        Ui.panel(surface, 10, 20, 100, 50);

        // The corner pixels are clear, and so is the one next to each along the edge.
        for (int[] corner : new int[][] {{10, 20}, {109, 20}, {10, 69}, {109, 69}}) {
            assertEquals(0, surface.colorAt(corner[0], corner[1]), () -> "a square corner at " + corner[0] + "," + corner[1]);
        }
        assertEquals(0, surface.colorAt(11, 20), "the curve starts on the very next pixel");
        assertEquals(UiTheme.INK, surface.colorAt(60, 20), "no ink line along the top");
        assertEquals(UiTheme.INK, surface.colorAt(10, 45), "no ink line down the side");
    }

    @Test
    void aPanelIsAStickerInkThenRimThenPaper() {
        Ui.panel(surface, 10, 20, 100, 50);

        assertEquals(UiTheme.INK, surface.colorAt(60, 20));
        assertEquals(UiTheme.HIGHLIGHT, surface.colorAt(60, 21), "no white rim inside the line");
        assertEquals(UiTheme.PANEL, surface.colorAt(60, 22));
        assertEquals(UiTheme.HIGHLIGHT, surface.colorAt(60, 68), "the rim stops short of the bottom");
        assertEquals(UiTheme.INK, surface.colorAt(60, 69));
    }

    @Test
    void aWellIsShadedAlongItsTopAndLeft() {
        Ui.well(surface, 0, 0, 18, 18);

        assertEquals(UiTheme.SHADE, surface.colorAt(9, 0), "no lip along the top");
        assertEquals(UiTheme.SHADE, surface.colorAt(0, 9), "no lip down the left");
        assertEquals(UiTheme.SURFACE, surface.colorAt(9, 9));
        assertEquals(UiTheme.SURFACE, surface.colorAt(17, 9), "the right edge is floor, not lip");
    }

    @Test
    void aWellKeepsToTheSizeItIsGiven() {
        Ui.well(surface, 4, 4, UiStyle.SLOT, UiStyle.SLOT);

        for (Rectangle rect : surface.rects) {
            assertTrue(rect.x() >= 4 && rect.x() + rect.width() <= 4 + UiStyle.SLOT,
                () -> "a well spilled out of its slot: " + rect);
        }
    }

    @Test
    void aCardCastsItsShadowBehindItself() {
        Ui.card(surface, 10, 20, 100, 50);

        assertEquals(UiTheme.SHADOW, surface.rects.get(0).argb(), "the shadow was not drawn first");
        assertEquals(UiTheme.SHADOW, surface.colorAt(10 + 100, 20 + 30), "no shadow past the right edge");
        assertEquals(UiTheme.INK, surface.colorAt(60, 20), "the panel is not over its shadow");
    }

    @Test
    void textRightEndsWhereItIsToldTo() {
        Ui.textRight(surface, "12 weeds", 200, 30, UiTheme.TEXT_MUTED);

        assertEquals(200 - surface.textWidth("12 weeds"), surface.texts.get(0).x());
    }

    @Test
    void aTooLongLineIsClippedToItsRoom() {
        Ui.textClipped(surface, "Hachiware", 0, 0, 30, UiTheme.TEXT);

        assertEquals("Hachi…", surface.texts.get(0).text());
    }

    @Test
    void anEmptyStateSitsInTheMiddleAndKeepsQuiet() {
        Ui.emptyState(surface, "nothing up today", 100, 40);

        Text line = surface.texts.get(0);
        assertEquals(100 - surface.textWidth("nothing up today") / 2, line.x());
        assertEquals(UiTheme.TEXT_MUTED, line.argb());
    }

    @Test
    void onlyTheHoveredRowIsTinted() {
        Ui.rowHighlight(surface, new Rect(4, 8, 100, UiStyle.ROW_H));

        assertEquals(UiTheme.HOVER, surface.colorAt(54, 8 + UiStyle.ROW_H / 2));
        assertEquals(0, surface.colorAt(4, 8), "the tint has square corners");
        assertEquals(0, surface.colorAt(104, 8 + UiStyle.ROW_H / 2), "the tint spilled past its row");
    }

    @Test
    void aRowKnowsWhetherTheCursorIsInIt() {
        Rect row = new Rect(10, 20, 100, UiStyle.ROW_H);

        assertTrue(row.contains(10, 20));
        assertTrue(row.contains(109, 20 + UiStyle.ROW_H - 1));
        assertFalse(row.contains(110, 25));
        assertFalse(row.contains(50, 20 + UiStyle.ROW_H));
    }
}
