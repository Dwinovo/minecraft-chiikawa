package com.dwinovo.chiikawa.ui;

/**
 * A box on screen. A list lays its rows out as these and asks which one the cursor is in,
 * so hit testing is worked out with the layout instead of guessed at again beside it.
 */
public record Rect(int x, int y, int width, int height) {
    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    /** The same box, moved: a layout written relative to a panel, placed on the screen. */
    public Rect offset(int dx, int dy) {
        return new Rect(x + dx, y + dy, width, height);
    }
}
