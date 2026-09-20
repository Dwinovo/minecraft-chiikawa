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
    void aPanelLeavesItsCornersEmptySoItReadsAsRounded() {
        Ui.panel(surface, 10, 20, 100, 50);

        for (Rectangle rect : surface.rects) {
            if (rect.argb() != UiTheme.BORDER) {
                continue;
            }
            assertFalse(rect.x() == 10 && rect.y() == 20, () -> "a pixel in the top-left corner: " + rect);
            assertFalse(rect.x() == 10 && rect.y() + rect.height() == 70, () -> "bottom-left corner: " + rect);
        }
        assertTrue(surface.rects.contains(new Rectangle(11, 21, 98, 48, UiTheme.PANEL)),
            () -> "no face inside the outline: " + surface.rects);
    }

    @Test
    void aPanelIsLitFromTheTopLeft() {
        Ui.panel(surface, 10, 20, 100, 50);

        assertEquals(new Rectangle(11, 21, 97, 1, UiTheme.HIGHLIGHT), surface.rectOf(UiTheme.HIGHLIGHT));
        assertEquals(new Rectangle(12, 68, 97, 1, UiTheme.SHADE), surface.rectOf(UiTheme.SHADE));
    }

    @Test
    void aWellIsTheSameLightTurnedOver() {
        Ui.well(surface, 0, 0, 18, 18);

        assertEquals(new Rectangle(0, 0, 18, 18, UiTheme.SURFACE), surface.rects.get(0));
        assertEquals(new Rectangle(0, 0, 17, 1, UiTheme.SHADE), surface.rectOf(UiTheme.SHADE));
        assertEquals(new Rectangle(1, 17, 17, 1, UiTheme.HIGHLIGHT), surface.rectOf(UiTheme.HIGHLIGHT));
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

        assertEquals(new Rectangle(10 + UiStyle.SHADOW_OFF, 20 + UiStyle.SHADOW_OFF, 100, 50, UiTheme.SHADOW),
            surface.rects.get(0));
        assertEquals(UiTheme.BORDER, surface.rects.get(1).argb());
    }

    @Test
    void aTitledPanelSaysWhereItsContentsStart() {
        int contentY = Ui.titledPanel(surface, 0, 0, 100, 80, "Labor Board");

        assertEquals(UiStyle.TITLE_H + UiStyle.PAD, contentY);
        assertEquals("Labor Board", surface.texts.get(0).text());
        assertTrue(surface.rects.contains(
                new Rectangle(UiStyle.BORDER, UiStyle.TITLE_H, 98, UiStyle.BORDER, UiTheme.DIVIDER)),
            () -> "no divider under the title: " + surface.rects);
    }

    @Test
    void aTitleSitsInTheMiddleOfItsBar() {
        Ui.titledPanel(surface, 0, 0, 100, 80, "Labor Board");

        assertEquals((UiStyle.TITLE_H - surface.lineHeight()) / 2, surface.texts.get(0).y());
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

        assertEquals(new Rectangle(4, 8, 100, UiStyle.ROW_H, UiTheme.HOVER), surface.rects.get(0));
        assertFalse(surface.rects.isEmpty());
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
