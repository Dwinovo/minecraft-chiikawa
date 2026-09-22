package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class PriceTest {
    /** Whatever money a pack has picked; the widget never looks inside it. */
    private static final Icon COIN = new Icon() {
    };

    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void theMoneyComesAfterTheWords() {
        Price.draw(surface, COIN, "Buy 3", 10, 0, UiStyle.CONTROL_H, UiTheme.TEXT);

        assertEquals("Buy 3", surface.texts.get(0).text());
        assertEquals(new RecordingSurface.DrawnIcon(COIN, 10 + surface.textWidth("Buy 3") + UiStyle.TIGHT,
            UiStyle.centerIn(0, UiStyle.CONTROL_H, UiStyle.ICON)), surface.icons.get(0));
    }

    @Test
    void itSaysHowWideItIsBeforeItIsDrawn() {
        assertEquals(Price.width(surface, "Upgrade 20"),
            Price.draw(surface, COIN, "Upgrade 20", 0, 0, UiStyle.CONTROL_H, UiTheme.TEXT));
    }

    @Test
    void centredIsCentredWithTheMoneyCounted() {
        Rect button = new Rect(0, 0, 60, UiStyle.CONTROL_H);
        Price.drawCentered(surface, COIN, "Sell 2", button, UiTheme.TEXT);

        int left = surface.texts.get(0).x();
        int right = left + Price.width(surface, "Sell 2");
        assertEquals(left, button.right() - right, "as much room on each side");
    }

    @Test
    void aPurseInTheCornerEndsAtTheEdge() {
        Price.drawRight(surface, COIN, "12", 100, 0, UiStyle.TITLE_H, UiTheme.TEXT_MUTED);

        assertEquals(100 - Price.width(surface, "12"), surface.texts.get(0).x());
        assertEquals(100 - UiStyle.ICON, surface.icons.get(0).x(), "the money is the last thing before the edge");
    }
}
