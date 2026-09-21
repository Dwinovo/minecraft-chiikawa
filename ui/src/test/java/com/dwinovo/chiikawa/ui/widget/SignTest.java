package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class SignTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void aShortNameStillHangsOnASign() {
        assertTrue(Sign.width(surface, "Hi") >= 48, "a two-letter name got a sign the size of a button");
    }

    @Test
    void theNameIsWrittenInTheMiddleOfTheBoard() {
        Sign.draw(surface, 10, 30, "Usagi");

        int width = Sign.width(surface, "Usagi");
        assertEquals(10 + (width - surface.textWidth("Usagi")) / 2, surface.texts.get(0).x());
        assertEquals(UiTheme.TEXT, surface.texts.get(0).argb());
    }

    @Test
    void theBoardHangsFromTwoStrings() {
        Sign.draw(surface, 10, 30, "Usagi");

        int width = Sign.width(surface, "Usagi");
        assertEquals(UiTheme.INK, surface.colorAt(20, 30 - Sign.STRING), "no string on the left");
        assertEquals(UiTheme.INK, surface.colorAt(10 + width - 11, 30 - Sign.STRING), "no string on the right");
        assertEquals(UiTheme.WOOD, surface.colorAt(10 + width / 2, 30 + Sign.HEIGHT / 2 + 3), "the board is not wood");
    }
}
