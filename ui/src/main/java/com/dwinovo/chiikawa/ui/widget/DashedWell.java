package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A well that is not there yet: a dashed outline where {@link com.dwinovo.chiikawa.ui.Ui#well} would be pressed into the
 * paper. Where something could go but nothing can be put yet, a bag's pockets with no bag worn say so by being drawn this
 * way, without a word on them.
 */
public final class DashedWell {
    /** A dash, and the gap after it. */
    private static final int DASH = 2;

    private DashedWell() {
    }

    public static void draw(DrawSurface surface, int x, int y, int width, int height) {
        for (int at = 0; at < width; at += 2 * DASH) {
            int length = Math.min(DASH, width - at);
            surface.fillRect(x + at, y, length, 1, UiTheme.SHADE);
            surface.fillRect(x + at, y + height - 1, length, 1, UiTheme.SHADE);
        }
        for (int at = 0; at < height; at += 2 * DASH) {
            int length = Math.min(DASH, height - at);
            surface.fillRect(x, y + at, 1, length, UiTheme.SHADE);
            surface.fillRect(x + width - 1, y + at, 1, length, UiTheme.SHADE);
        }
    }
}
