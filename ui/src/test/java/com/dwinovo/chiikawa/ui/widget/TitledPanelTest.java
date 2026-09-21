package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class TitledPanelTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void itSaysWhereItsContentsStart() {
        assertEquals(20 + UiStyle.TITLE_H + UiStyle.PAD, TitledPanel.draw(surface, 10, 20, 150, 80, "Shop"));
    }

    @Test
    void theNameIsOnABoardHungAcrossTheTopEdge() {
        TitledPanel.draw(surface, 10, 20, 150, 80, "Shop");

        RecordingSurface.Text name = surface.texts.get(0);
        assertEquals("Shop", name.text());
        // The board straddles the edge: its text sits across the card's top line.
        assertEquals(true, name.y() < 20 && name.y() + surface.lineHeight() > 20,
            "the name is not written across the top edge: " + name);
    }

    @Test
    void aDashedLineSaysWhereTheContentsStart() {
        TitledPanel.draw(surface, 10, 20, 150, 80, "Shop");

        assertEquals(UiTheme.DIVIDER, surface.colorAt(10 + UiStyle.PAD, 20 + UiStyle.TITLE_H));
        assertEquals(UiTheme.PANEL, surface.colorAt(10 + UiStyle.PAD + 2, 20 + UiStyle.TITLE_H), "the line is not dashed");
    }
}
