package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A pet's health, as the row of hearts a player already knows how to read. Half a heart
 * means half a heart, and a heart spent stays on the row in the shade, so the row's length
 * says how tough the pet is and how much of it is left at the same time.
 *
 * <p>Drawn rather than blitted, in the manga's line: an ink outline round a pink heart with
 * a white glint, a spent one pale inside the same outline. Seven pixels of shape, and an
 * image would be one more file to keep in step with the palette.
 */
public final class Hearts {
    /** Widest and tallest the shape gets. */
    public static final int SIZE = 7;
    /** Step from one heart to the next, a pixel of air between them. */
    public static final int PITCH = SIZE + 1;
    /** The columns a half heart fills. */
    private static final int HALF = 4;

    /** The shape: X is the ink line, o the inside. */
    private static final String[] SHAPE = {
        ".XX.XX.",
        "XooXooX",
        "XoooooX",
        ".XoooX.",
        "..XoX..",
        "...X...",
    };

    private Hearts() {
    }

    /** How wide {@link #draw} is for a pet with this much health. */
    public static int width(int maxHealth) {
        return count(maxHealth) * PITCH - 1;
    }

    /** One heart for every two points, the way the game counts them. */
    public static int count(int maxHealth) {
        return Math.max(1, (Math.max(1, maxHealth) + 1) / 2);
    }

    public static void draw(DrawSurface surface, int x, int y, int health, int maxHealth) {
        int hearts = count(maxHealth);
        for (int i = 0; i < hearts; i++) {
            int left = health - i * 2;
            heart(surface, x + i * PITCH, y, left >= 2 ? SIZE : left == 1 ? HALF : 0);
        }
    }

    /** One heart, pink up to column {@code filled} and pale after it. */
    private static void heart(DrawSurface surface, int x, int y, int filled) {
        for (int row = 0; row < SHAPE.length; row++) {
            String line = SHAPE[row];
            for (int column = 0; column < line.length(); column++) {
                char c = line.charAt(column);
                if (c == 'X') {
                    surface.fillRect(x + column, y + row, 1, 1, UiTheme.INK);
                } else if (c == 'o') {
                    surface.fillRect(x + column, y + row, 1, 1, column < filled ? UiTheme.LIFE : UiTheme.SURFACE);
                }
            }
        }
        if (filled > 1) {
            surface.fillRect(x + 1, y + 1, 1, 1, UiTheme.HIGHLIGHT);
        }
    }
}
