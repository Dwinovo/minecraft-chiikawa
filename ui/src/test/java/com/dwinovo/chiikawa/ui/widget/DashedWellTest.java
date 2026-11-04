package com.dwinovo.chiikawa.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.ui.RecordingSurface;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import org.junit.jupiter.api.Test;

class DashedWellTest {
    private final RecordingSurface surface = new RecordingSurface();

    @Test
    void onlyTheOutlineIsDrawnAndTheMiddleIsLeftToWhatIsBehindIt() {
        DashedWell.draw(surface, 10, 20, UiStyle.SLOT, UiStyle.SLOT);

        assertEquals(0, surface.colorAt(10 + UiStyle.SLOT / 2, 20 + UiStyle.SLOT / 2));
        assertEquals(UiTheme.SHADE, surface.colorAt(10, 20), "no dash at the corner");
    }

    @Test
    void theLineIsDashedAndNotSolid() {
        DashedWell.draw(surface, 10, 20, UiStyle.SLOT, UiStyle.SLOT);

        assertEquals(UiTheme.SHADE, surface.colorAt(11, 20));
        assertEquals(0, surface.colorAt(12, 20), "no gap in the dashes");
        assertEquals(0, surface.colorAt(10, 22));
    }

    @Test
    void everyEdgeIsDrawnInsideTheBoxItIsGiven() {
        DashedWell.draw(surface, 10, 20, UiStyle.SLOT, UiStyle.SLOT);

        for (RecordingSurface.Rectangle rect : surface.rects) {
            assertEquals(true, rect.x() >= 10 && rect.y() >= 20 && rect.x() + rect.width() <= 10 + UiStyle.SLOT
                && rect.y() + rect.height() <= 20 + UiStyle.SLOT, rect.toString());
        }
    }
}
