package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class ChipTest {
    private final RecordingSurface surface = new RecordingSurface();
    private final Icon icon = new Icon() {
    };

    @Test
    void aPlainChipSitsOnItsLineCentredOnTheSpotItIsGiven() {
        Chip chip = Chip.of("Idle");
        chip.draw(surface, 100, 50);

        int width = 4 * 5 + 2 * UiStyle.PAD;
        int height = 9 + 2 * UiStyle.CHIP_PAD;
        assertEquals(width, chip.width(surface));
        // A sticker from its left edge to its right, sat on the line it was given.
        assertEquals(UiTheme.INK, surface.colorAt(100, 50 - height), "no ink along the chip's top");
        assertEquals(UiTheme.INK, surface.colorAt(100, 49), "the chip does not sit on its line");
        assertEquals(0, surface.colorAt(100, 50), "the chip hangs below its line");
        assertEquals(UiTheme.PANEL, surface.colorAt(100 - width / 2 + 3, 50 - height / 2));
        assertEquals("Idle", surface.texts.get(0).text());
    }

    @Test
    void aPlainChipCarriesNeitherIconNorBar() {
        Chip.of("Idle").draw(surface, 0, 0);

        assertTrue(surface.icons.isEmpty());
        assertFalse(surface.hasRectOf(UiTheme.LEAF));
    }

    @Test
    void aWorkingChipMakesRoomForItsIconAndItsBar() {
        Chip chip = Chip.working(icon, "Weeding", 3, 11);

        assertEquals(2 * UiStyle.PAD + 7 * 5 + UiStyle.SLOT + UiStyle.GAP + UiStyle.GAP + Chip.BAR_W,
            chip.width(surface));
        assertEquals(UiStyle.SLOT + 2 * UiStyle.CHIP_PAD, chip.height(surface));
    }

    @Test
    void theIconLeadsAndTheBarTrails() {
        Chip chip = Chip.working(icon, "Weeding", 3, 11);
        chip.draw(surface, 0, 0);

        int left = -chip.width(surface) / 2 + UiStyle.PAD;
        assertEquals(left + UiStyle.BORDER, surface.icons.get(0).x());
        assertEquals(left + UiStyle.SLOT + UiStyle.GAP, surface.texts.get(0).x());
        assertTrue(surface.hasRectOf(UiTheme.LEAF), () -> "no bar on a working chip: " + surface.rects);
    }
}
