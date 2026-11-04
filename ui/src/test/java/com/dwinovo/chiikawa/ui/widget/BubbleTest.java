package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class BubbleTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void theCardFitsItsWordsWithTheTailUnderIt() {
        Bubble bubble = new Bubble("Wah");
        bubble.draw(surface, 10, 20, 20);

        int width = 3 * 5 + 2 * UiStyle.GAP;
        int height = 9 + 2 * UiStyle.TIGHT;
        assertEquals(width, bubble.width(surface));
        assertEquals(height, bubble.height(surface));
        assertEquals(UiTheme.PANEL, surface.colorAt(10 + width / 2, 20 + height / 2));
        assertEquals(UiTheme.INK, surface.colorAt(20, 20 + height), "no tail under the card");
        assertEquals(UiTheme.INK, surface.colorAt(20, 20 + height + Bubble.TAIL - 1), "the tail is short of its tip");
        assertEquals(0, surface.colorAt(20, 20 + height + Bubble.TAIL), "the tail hangs too far");
        assertEquals(10 + UiStyle.GAP, surface.texts.get(0).x());
    }

    @Test
    void theTailStaysUnderTheCardWhenTheSpeakerIsOffToTheSide() {
        Bubble bubble = new Bubble("Wah");
        bubble.draw(surface, 10, 20, 0);

        int height = bubble.height(surface);
        assertEquals(UiTheme.INK, surface.colorAt(13, 20 + height + 1), "the tail left the card's side");
        assertEquals(0, surface.colorAt(0, 20 + height + 1));
    }

    @Test
    void aFramedCardHasItsTailOutOfItsLeftSideAtTheSpeakersHeight() {
        Rect card = new Rect(30, 20, 100, 19);
        Bubble.frame(surface, card, 29);

        assertEquals(UiTheme.PANEL, surface.colorAt(80, 29), "the card is not paper");
        assertEquals(UiTheme.INK, surface.colorAt(card.x() - Bubble.SIDE_TAIL, 29), "no tip to the tail");
        assertEquals(UiTheme.PANEL, surface.colorAt(card.x() - 1, 29), "the tail is shut off from the card");
        assertEquals(0, surface.colorAt(card.x() - Bubble.SIDE_TAIL - 1, 29), "the tail reaches too far");
        assertEquals(0, surface.colorAt(card.x() - Bubble.SIDE_TAIL, 25), "the tail is not a point");
    }

    @Test
    void theSideTailStaysOnTheCardWhenTheSpeakerIsAboveOrBelowIt() {
        Rect card = new Rect(30, 20, 100, 19);
        Bubble.frame(surface, card, 0);

        assertEquals(UiTheme.INK, surface.colorAt(card.x() - Bubble.SIDE_TAIL, card.y() + 4), "the tail left the card's side");
    }
}
