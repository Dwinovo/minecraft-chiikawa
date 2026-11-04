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

class TabStripTest {
    private static final Rect PANEL = new Rect(20, 40, 160, 80);
    private final RecordingSurface surface = new RecordingSurface();
    private final TabStrip strip = new TabStrip(PANEL, List.of(
        new TabStrip.Tab((s, x, y) -> { }, "Bag"), new TabStrip.Tab((s, x, y) -> { }, "Settings")), surface::textWidth);

    @Test
    void tabsStandAtThePanelsRightEndEachAsWideAsItsName() {
        Rect last = strip.bounds(1, false);

        assertEquals(PANEL.right() - TabStrip.INSET, last.right(), "the last tab does not end at the inset");
        assertEquals(TabStrip.width(surface::textWidth, "Settings"), last.width());
        assertEquals(strip.bounds(0, false).right() + TabStrip.GAP, last.x(), "the tabs are not a gap apart");
        assertTrue(strip.bounds(0, false).width() < last.width(), "a short name has as wide a tab as a long one");
    }

    @Test
    void aTabIsFoundByTheColumnAboveThePanel() {
        Rect first = strip.bounds(0, false);

        assertEquals(0, strip.at(first.x() + 2, PANEL.y() - 2));
        // Just above a lower tab still counts: it is the same column.
        assertEquals(0, strip.at(first.x() + 2, PANEL.y() - TabStrip.HEIGHT));
        assertEquals(-1, strip.at(first.x() + 2, PANEL.y()), "the panel itself is not a tab");
        assertEquals(-1, strip.at(first.x() - 1, PANEL.y() - 2), "beside the strip is not a tab");
        assertEquals(-1, strip.at(first.right(), PANEL.y() - 2), "the gap between tabs is not a tab");
    }

    @Test
    void theTabsThatAreNotPickedShowWhole() {
        Rect lower = strip.bounds(1, false);

        assertTrue(lower.y() + 2 + UiStyle.ICON <= PANEL.y(), "an unpicked tab's picture is cut by the panel");
    }

    @Test
    void thePickedTabIsOnePieceWithTheBandItStandsOn() {
        strip.drawBehind(surface, 0);
        Ui.panel(surface, PANEL.x(), PANEL.y(), PANEL.width(), PANEL.height());
        strip.drawFront(surface, 0, UiTheme.ACCENT_PALE);

        int pickedMiddle = strip.bounds(0, true).x() + 3 * strip.bounds(0, true).width() / 4;
        int otherMiddle = strip.bounds(1, false).x() + strip.bounds(1, false).width() / 2;
        assertEquals(UiTheme.ACCENT_PALE, surface.colorAt(pickedMiddle, PANEL.y()), "a line between the picked tab and its panel");
        assertEquals(UiTheme.INK, surface.colorAt(otherMiddle, PANEL.y()), "an unpicked tab ran into the panel");
        assertEquals(UiTheme.SURFACE, surface.colorAt(otherMiddle, PANEL.y() - 8), "an unpicked tab lost its paper");
    }

    @Test
    void eachTabIsNamedBesideItsPictureAndThePickedOneInTheInk() {
        strip.drawBehind(surface, 0);
        strip.drawFront(surface, 0, UiTheme.ACCENT_PALE);

        assertEquals(List.of("Settings", "Bag"), surface.texts.stream().map(RecordingSurface.Text::text).toList());
        assertEquals(UiTheme.TEXT_MUTED, surface.texts.get(0).argb());
        assertEquals(UiTheme.TEXT, surface.texts.get(1).argb());
        Rect picked = strip.bounds(0, true);
        assertTrue(surface.texts.get(1).x() >= picked.x() + UiStyle.ICON, "the name is not beside the picture");
        assertTrue(surface.texts.get(1).x() + surface.textWidth("Bag") <= picked.right(), "the name runs out of its tab");
    }
}
