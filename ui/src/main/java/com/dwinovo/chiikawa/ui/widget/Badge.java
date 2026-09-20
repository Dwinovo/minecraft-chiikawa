package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A word in a well: a state, not a sentence. "Taken" in a badge is read as a state of the
 * row it sits in; "Taken by Usagi" written out as a line is read as prose, and prose in a
 * list has to be read line by line before the list can be scanned at all.
 *
 * <p>The colour is in the word, not behind it. A row of filled badges shouting in three
 * colours is louder than the content they label — the quiet well says "this is a state"
 * and the ink says which one.
 */
public final class Badge {
    private Badge() {
    }

    /** What {@link #draw} will take up, for a caller laying out from the right edge. */
    public static int width(DrawSurface surface, String text) {
        return surface.textWidth(text) + 2 * UiStyle.GAP;
    }

    public static int height(DrawSurface surface) {
        return surface.lineHeight() + 2 * UiStyle.TIGHT;
    }

    /**
     * @param argb the ink; {@link UiTheme#TEXT_MUTED} for a state that is merely true,
     *             {@link UiTheme#SUCCESS} for one worth noticing
     * @return the width it took, so a caller can place the next one
     */
    public static int draw(DrawSurface surface, String text, int x, int y, int argb) {
        int width = width(surface, text);
        surface.fillRect(x, y, width, height(surface), UiTheme.SURFACE);
        surface.drawText(text, x + UiStyle.GAP, y + UiStyle.TIGHT, argb);
        return width;
    }
}
