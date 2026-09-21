package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class BadgeTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void aBadgeSaysHowWideItIsBeforeItIsDrawn() {
        assertEquals(Badge.width(surface, "taken"), Badge.draw(surface, "taken", 0, 0, UiTheme.SUCCESS));
    }

    @Test
    void theWordSitsInTheMiddleOfItsWell() {
        Badge.draw(surface, "open", 30, 10, UiTheme.TEXT_MUTED);

        assertEquals(new RecordingSurface.Text("open", 30 + UiStyle.GAP, 10 + UiStyle.TIGHT, UiTheme.TEXT),
            surface.texts.get(0));
        assertEquals(UiTheme.INK, surface.colorAt(40, 10), "a badge is a sticker, with the ink line round it");
        assertEquals(UiTheme.pale(UiTheme.TEXT_MUTED), surface.colorAt(40, 15), "the state is in the fill");
    }
}
