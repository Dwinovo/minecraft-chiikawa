package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
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
     * A rounded track in the ink line, green as far as the work has got. Only a finished
     * bar is drawn full, and in the darker green: a bar that is nearly there never says it
     * is.
     *
     * @param done how much is done
     * @param total how much there is; nothing is drawn but the track when it is 0
     */
    public static void draw(DrawSurface surface, int x, int y, int width, int height, int done, int total) {
        int radius = height / 2;
        Ui.roundRect(surface, x, y, width, height, radius, UiTheme.INK);
        int innerW = width - 2;
        int innerH = height - 2;
        Ui.roundRect(surface, x + 1, y + 1, innerW, innerH, Math.max(0, radius - 1), UiTheme.SURFACE);
        if (total <= 0 || done <= 0 || innerW <= 0) {
            return;
        }
        if (done >= total) {
            Ui.roundRect(surface, x + 1, y + 1, innerW, innerH, Math.max(0, radius - 1), UiTheme.SUCCESS);
            return;
        }
        int fill = Math.min(innerW - 1, Math.max(MIN_FILL, (int) ((long) innerW * done / total)));
        Ui.roundRect(surface, x + 1, y + 1, fill, innerH, Math.max(0, Math.min(radius - 1, fill / 2)), UiTheme.LEAF);
    }
}
