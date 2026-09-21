package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class TabsTest {
    private static final int PANEL_X = 20;
    private static final int PANEL_Y = 40;
    private final RecordingSurface surface = new RecordingSurface();
    private final List<Tabs.Face> faces = List.of((s, x, y) -> { }, (s, x, y) -> { }, (s, x, y) -> { });
    private final int[] tints = {UiTheme.ACCENT_PALE, UiTheme.SKY_PALE, UiTheme.LEAF_PALE};

    @Test
    void tabsRunLeftToRightFromTheInset() {
        assertEquals(PANEL_X + Tabs.INSET, Tabs.bounds(PANEL_X, PANEL_Y, 0, false).x());
        assertEquals(PANEL_X + Tabs.INSET + Tabs.WIDTH + Tabs.GAP, Tabs.bounds(PANEL_X, PANEL_Y, 1, false).x());
    }

    @Test
    void aTabIsFoundByTheColumnAboveThePanel() {
        Rect second = Tabs.bounds(PANEL_X, PANEL_Y, 1, false);

        assertEquals(1, Tabs.at(PANEL_X, PANEL_Y, 3, second.x() + 2, PANEL_Y - 2));
        // Just above a lower tab still counts: it is the same column.
        assertEquals(1, Tabs.at(PANEL_X, PANEL_Y, 3, second.x() + 2, PANEL_Y - Tabs.HEIGHT));
        assertEquals(-1, Tabs.at(PANEL_X, PANEL_Y, 3, second.x() + 2, PANEL_Y), "the panel itself is not a tab");
        assertEquals(-1, Tabs.at(PANEL_X, PANEL_Y, 3, second.x() - 1, PANEL_Y - 2), "the gap between tabs is not a tab");
    }

    @Test
    void theTabsThatAreNotPickedShowWhole() {
        Rect lower = Tabs.bounds(PANEL_X, PANEL_Y, 2, false);

        assertTrue(lower.y() + Tabs.FACE_TOP + UiStyle.ICON <= PANEL_Y, "an unpicked tab's picture is cut by the panel");
    }

    @Test
    void thePickedTabIsOnePieceWithThePanel() {
        Tabs.drawBehind(surface, PANEL_X, PANEL_Y, faces, tints, 0);
        Ui.panel(surface, PANEL_X, PANEL_Y, 160, 80);
        Tabs.drawFront(surface, PANEL_X, PANEL_Y, faces.get(0), 0);

        int pickedMiddle = Tabs.bounds(PANEL_X, PANEL_Y, 0, true).x() + Tabs.WIDTH / 2;
        int otherMiddle = Tabs.bounds(PANEL_X, PANEL_Y, 1, false).x() + Tabs.WIDTH / 2;
        assertEquals(UiTheme.PANEL, surface.colorAt(pickedMiddle, PANEL_Y), "a line between the picked tab and its panel");
        assertEquals(UiTheme.INK, surface.colorAt(otherMiddle, PANEL_Y), "an unpicked tab ran into the panel");
        assertEquals(UiTheme.SKY_PALE, surface.colorAt(otherMiddle, PANEL_Y - 8), "an unpicked tab lost its colour");
    }
}
