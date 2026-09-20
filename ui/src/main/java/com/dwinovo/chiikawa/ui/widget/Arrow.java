package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * The triangle on a page button. Seven rows of pixels: at this size a drawn arrow is
 * plainer than any word for it, and it needs no translating.
 */
public final class Arrow {
    public static final int WIDTH = 4;
    public static final int HEIGHT = 7;

    private Arrow() {
    }

    /** Draws it in the middle of {@code area}, pointing the way asked. */
    public static void draw(DrawSurface surface, Rect area, boolean pointsLeft, int argb) {
        int x = area.x() + (area.width() - WIDTH) / 2;
        int y = UiStyle.centerIn(area.y(), area.height(), HEIGHT);
        for (int row = 0; row < HEIGHT; row++) {
            // 1, 2, 3, 4, 3, 2, 1 — the point of the triangle is the widest row's far end.
            int depth = Math.min(row, HEIGHT - 1 - row) + 1;
            surface.fillRect(pointsLeft ? x + WIDTH - depth : x, y + row, depth, 1, argb);
        }
    }
}
