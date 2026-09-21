package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class PriceTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void anEmeraldComesAfterTheWords() {
        Price.draw(surface, "Buy 3", 10, 0, UiStyle.CONTROL_H, UiTheme.TEXT);

        RecordingSurface.Text words = surface.texts.get(0);
        assertEquals("Buy 3", words.text());
        int emeraldX = 10 + surface.textWidth("Buy 3") + UiStyle.TIGHT;
        assertEquals(UiTheme.INK, surface.colorAt(emeraldX, words.y() + 2), "the emerald's ink line starts after the words");
        assertEquals(UiTheme.EMERALD, surface.colorAt(emeraldX + 4, words.y() + 2), "and it is an emerald");
        assertEquals(0, surface.colorAt(emeraldX, words.y() + PixelArt.EMERALD.height()),
            "no taller than the words' capitals");
    }

    @Test
    void itSaysHowWideItIsBeforeItIsDrawn() {
        assertEquals(Price.width(surface, "Upgrade 20"),
            Price.draw(surface, "Upgrade 20", 0, 0, UiStyle.CONTROL_H, UiTheme.TEXT));
    }

    @Test
    void centredIsCentredWithTheEmeraldCounted() {
        Rect button = new Rect(0, 0, 60, UiStyle.CONTROL_H);
        Price.drawCentered(surface, "Sell 2", button, UiTheme.TEXT);

        int left = surface.texts.get(0).x();
        int right = left + Price.width(surface, "Sell 2");
        assertEquals(left, button.right() - right, "as much room on each side");
    }

    @Test
    void aPurseInTheCornerEndsAtTheEdge() {
        Price.drawRight(surface, "12", 100, 0, UiStyle.TITLE_H, UiTheme.TEXT_MUTED);

        assertEquals(100 - Price.width(surface, "12"), surface.texts.get(0).x());
        assertEquals(UiTheme.INK, surface.colorAt(100 - 1, surface.texts.get(0).y() + 2),
            "the emerald's far side is the last thing before the edge");
    }
}
