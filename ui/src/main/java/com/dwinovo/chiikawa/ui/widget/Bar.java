package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * How far along something is, as a length instead of as two numbers. "7/11" has to be read
 * and divided; a bar is read at a glance and from across the room, which is what a label
 * over a pet's head has to survive.
 */
public final class Bar {
    /**
     * The narrowest a started bar may be drawn. Rounding a real first step down to nothing
     * would have the picture say no work has been done when some has.
     */
    public static final int MIN_FILL = 1;

    private Bar() {
    }

    /**
     * @param done how much is finished
     * @param total how much finishes it; nothing is filled when it is not positive
     */
    public static void draw(DrawSurface surface, int x, int y, int width, int height, int done, int total) {
        surface.fillRect(x, y, width, height, UiTheme.SURFACE);
        if (total <= 0 || done <= 0 || width <= 0) {
            return;
        }
        if (done >= total) {
            surface.fillRect(x, y, width, height, UiTheme.SUCCESS);
            return;
        }
        // Only a finished bar is drawn full, so a bar that is nearly there never says it is.
        int fill = Math.min(width - 1, Math.max(MIN_FILL, (int) ((long) width * done / total)));
        surface.fillRect(x, y, fill, height, UiTheme.ACCENT);
    }
}
