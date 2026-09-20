package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A pet's health, as the row of hearts a player already knows how to read. Half a heart
 * means half a heart, and a heart spent stays on the row in the shade, so the row's length
 * says how tough the pet is and how much of it is left at the same time.
 *
 * <p>Drawn rather than blitted: it is seven pixels of shape, and an image would be one more
 * file to keep in step with the palette.
 */
public final class Hearts {
    /** Widest and tallest the shape gets. */
    public static final int SIZE = 7;
    /** Step from one heart to the next, a pixel of air between them. */
    public static final int PITCH = SIZE + 1;
    /** Where the left half of a heart ends, for a half-empty one. */
    private static final int HALF = 3;

    /** The shape, row by row: pairs of (start, length) runs. */
    private static final int[][] ROWS = {
        {1, 2, 4, 2},
        {0, 7},
        {0, 7},
        {1, 5},
        {2, 3},
        {3, 1},
    };

    private Hearts() {
    }

    /** How wide a row for this much health comes out. */
    public static int width(int maxHealth) {
        return count(maxHealth) * PITCH - 1;
    }

    /** How many hearts a pet with this much health at most has. */
    public static int count(int maxHealth) {
        return Math.max(1, (Math.max(1, maxHealth) + 1) / 2);
    }

    /**
     * @param health what the pet has left, in half hearts
     * @param maxHealth what it has when well, in half hearts
     */
    public static void draw(DrawSurface surface, int x, int y, int health, int maxHealth) {
        int hearts = count(maxHealth);
        for (int i = 0; i < hearts; i++) {
            int left = health - i * 2;
            heart(surface, x + i * PITCH, y, left >= 2 ? SIZE : left == 1 ? HALF : 0);
        }
    }

    /** One heart with its leftmost {@code filled} pixels in colour and the rest in shade. */
    private static void heart(DrawSurface surface, int x, int y, int filled) {
        for (int row = 0; row < ROWS.length; row++) {
            int[] runs = ROWS[row];
            for (int run = 0; run < runs.length; run += 2) {
                int start = runs[run];
                int length = runs[run + 1];
                int lit = Math.min(length, Math.max(0, filled - start));
                if (lit > 0) {
                    surface.fillRect(x + start, y + row, lit, 1, UiTheme.LIFE);
                }
                if (lit < length) {
                    surface.fillRect(x + start + lit, y + row, length - lit, 1, UiTheme.SHADE);
                }
            }
        }
    }
}
