package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A word in a well: a state, not a sentence. "Taken" in a badge is read as a state of the
 * row it sits in; "Taken by Usagi" written out as a line is read as prose, and prose in a
 * list has to be read line by line before the list can be scanned at all.
 *
 * <p>A small sticker in the ink line, the colour washed pale behind ink-coloured words.
 * Saturated fills in a list shout louder than the rows they label; a pale tint says which
 * state it is and leaves the words readable.
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
    /**
     * @param argb the state's colour; the badge is filled with a pale wash of it
     * @return the width it took
     */
    public static int draw(DrawSurface surface, String text, int x, int y, int argb) {
        int width = width(surface, text);
        Ui.sticker(surface, x, y, width, height(surface), Ui.WELL_RADIUS + 1, UiTheme.pale(argb));
        surface.drawText(text, x + UiStyle.GAP, y + UiStyle.TIGHT, UiTheme.TEXT);
        return width;
    }
}
