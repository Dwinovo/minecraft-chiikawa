package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A band of colour across the top of a panel, with the ink line along its bottom edge: the part of a panel that says who
 * it is about, over the part that says what they have. Dotted, faintly, like patterned paper.
 *
 * <p>It is drawn inside the panel, over the panel's own paper, and its bottom line runs on into the panel's outline, so
 * the two are one drawing.
 */
public final class Banner {
    /** How round the band's top corners are: the panel's, less the line and the rim. */
    private static final int RADIUS = Ui.PANEL_RADIUS - 2;
    /** The step between two dots, along and down. */
    private static final int DOT_PITCH = 10;

    private Banner() {
    }

    /**
     * @param x where the band starts, inside the panel's rim
     * @param y where the band starts, inside the panel's rim
     * @param width how wide it is, short of the rim on both sides
     * @param height how tall it is, its bottom line included
     * @param fill the band's colour
     */
    public static void draw(DrawSurface surface, int x, int y, int width, int height, int fill) {
        // Round at the top, where the panel is, and square at the bottom, where it meets the rest of it.
        Ui.roundRect(surface, x, y, width, height, RADIUS, fill);
        surface.fillRect(x, y + height - RADIUS, width, RADIUS, fill);
        for (int row = 0; (row + 1) * DOT_PITCH < height; row++) {
            int shift = row % 2 == 0 ? 0 : DOT_PITCH / 2;
            for (int dotX = DOT_PITCH / 2 + shift; dotX < width - 1; dotX += DOT_PITCH) {
                surface.fillRect(x + dotX, y + DOT_PITCH / 2 + row * DOT_PITCH, 1, 1, UiTheme.DOTS);
            }
        }
        // Out over the rim to the outline either side, so that the line is not a line in a box.
        surface.fillRect(x - 2, y + height - 1, width + 4, 1, UiTheme.INK);
    }
}
