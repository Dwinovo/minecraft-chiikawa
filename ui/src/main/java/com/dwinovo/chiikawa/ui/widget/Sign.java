package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A name written on a small wooden board, hung from two strings — the labor board's own
 * material, and the way Chiikawa Pocket labels everything that is a name rather than a
 * sentence. Hung across a panel's top edge it reads as the panel's title without taking a
 * row of the panel to say it.
 */
public final class Sign {
    /** How tall the board is. */
    public static final int HEIGHT = 16;
    /** How far above the board its strings go. */
    public static final int STRING = 5;
    /** The narrowest a board gets, so a short name still hangs like a sign. */
    private static final int MIN_WIDTH = 48;
    /** Where the strings meet the board, in from each end. */
    private static final int STRING_IN = 10;

    private Sign() {
    }

    public static int width(DrawSurface surface, String text) {
        return Math.max(MIN_WIDTH, surface.textWidth(text) + 2 * UiStyle.PAD + 2 * UiStyle.TIGHT);
    }

    /** A board with its top-left corner at x, y, the strings rising above it. */
    public static void draw(DrawSurface surface, int x, int y, String text) {
        int width = width(surface, text);
        surface.fillRect(x + STRING_IN, y - STRING, 1, STRING, UiTheme.INK);
        surface.fillRect(x + width - 1 - STRING_IN, y - STRING, 1, STRING, UiTheme.INK);
        Ui.roundRect(surface, x, y, width, HEIGHT, Ui.WELL_RADIUS, UiTheme.INK);
        Ui.roundRect(surface, x + 1, y + 1, width - 2, HEIGHT - 2, Ui.WELL_RADIUS - 1, UiTheme.WOOD_DARK);
        Ui.roundRect(surface, x + 1, y + 1, width - 3, HEIGHT - 3, Ui.WELL_RADIUS - 1, UiTheme.WOOD_LIGHT);
        surface.fillRect(x + 2, y + 2, width - 4, HEIGHT - 4, UiTheme.WOOD);
        // A little grain, so it is a board and not a button.
        surface.fillRect(x + 4, y + HEIGHT - 4, 8, 1, UiTheme.WOOD_DARK);
        surface.fillRect(x + width - 14, y + HEIGHT - 5, 6, 1, UiTheme.WOOD_DARK);
        surface.drawText(text, x + (width - surface.textWidth(text)) / 2,
            UiStyle.centerIn(y, HEIGHT, surface.lineHeight()), UiTheme.TEXT);
    }
}
