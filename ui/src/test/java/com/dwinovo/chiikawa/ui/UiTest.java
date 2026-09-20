package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void aPanelIsItsOutlineWithTheBodyInsideIt() {
        Ui.panel(surface, 10, 20, 100, 50);

        assertEquals(List.of(
            new Rect(10, 20, 100, 50, UiTheme.BORDER),
            new Rect(11, 21, 98, 48, UiTheme.PANEL)
        ), surface.rects);
    }

    @Test
    void aTitledPanelSaysWhereItsContentsStart() {
        int contentY = Ui.titledPanel(surface, 0, 0, 100, 80, "Labor Board");

        assertEquals(UiStyle.TITLE_H + UiStyle.PAD, contentY);
        assertEquals(List.of(new Text("Labor Board", UiStyle.PAD, UiTheme.TEXT)), surface.texts);
        assertTrue(surface.rects.contains(new Rect(UiStyle.BORDER, UiStyle.TITLE_H, 98, UiStyle.BORDER, UiTheme.DIVIDER)),
            () -> "no divider under the title: " + surface.rects);
    }

    @Test
    void textRightEndsWhereItIsToldTo() {
        Ui.textRight(surface, "12 weeds", 200, 30, UiTheme.TEXT_MUTED);

        assertEquals(200 - surface.textWidth("12 weeds"), surface.texts.get(0).x());
    }

    @Test
    void aChipSitsOnItsLineCentredOnTheSpotItIsGiven() {
        Ui.chip(surface, "Idle", 100, 50);

        int width = 4 * 5 + 2 * UiStyle.PAD;
        int height = 9 + 2 * UiStyle.CHIP_PAD;
        assertEquals(new Rect(100 - width / 2, 50 - height, width, height, UiTheme.BORDER), surface.rects.get(0));
        assertEquals(new Text("Idle", 100 - width / 2 + UiStyle.PAD, UiTheme.TEXT), surface.texts.get(0));
    }

    @Test
    void aTooLongLineIsClippedToItsRoom() {
        Ui.textClipped(surface, "Hachiware", 0, 0, 30, UiTheme.TEXT);

        assertEquals("Hachi…", surface.texts.get(0).text());
    }

    private record Rect(int x, int y, int width, int height, int argb) {
    }

    private record Text(String text, int x, int argb) {
    }

    /** Stands in for the game's drawing API: five pixels a character. */
    private static final class RecordingSurface implements DrawSurface {
        private final List<Rect> rects = new ArrayList<>();
        private final List<Text> texts = new ArrayList<>();

        @Override
        public void fillRect(int x, int y, int width, int height, int argb) {
            rects.add(new Rect(x, y, width, height, argb));
        }

        @Override
        public void drawText(String text, int x, int y, int argb) {
            texts.add(new Text(text, x, argb));
        }

        @Override
        public int textWidth(String text) {
            return text.length() * 5;
        }

        @Override
        public int lineHeight() {
            return 9;
        }
    }
}
