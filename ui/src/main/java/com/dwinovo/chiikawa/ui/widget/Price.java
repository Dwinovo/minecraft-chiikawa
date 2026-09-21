package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * Words that end in an amount of emeralds — "Buy 3", "Upgrade 20", or only the number —
 * with a small emerald after them, so a number on a button or in a corner reads as money
 * rather than as a count of the thing beside it.
 */
public final class Price {
    private Price() {
    }

    /** What {@link #draw} takes up for words {@code textWidth} wide, for laying out before drawing. */
    public static int width(int textWidth) {
        return textWidth + UiStyle.TIGHT + PixelArt.EMERALD.width();
    }

    public static int width(DrawSurface surface, String text) {
        return width(surface.textWidth(text));
    }

    /**
     * Draws it from {@code x}, in the middle of a box {@code height} tall from {@code y}. The
     * emerald stands on the words' line, as tall as their capitals.
     *
     * @return the width it took
     */
    public static int draw(DrawSurface surface, String text, int x, int y, int height, int argb) {
        int textY = UiStyle.centerIn(y, height, surface.lineHeight());
        surface.drawText(text, x, textY, argb);
        PixelArt.EMERALD.draw(surface, x + surface.textWidth(text) + UiStyle.TIGHT, textY);
        return width(surface, text);
    }

    /** In the middle of a box — what a button's face wants. */
    public static void drawCentered(DrawSurface surface, String text, Rect area, int argb) {
        draw(surface, text, area.x() + (area.width() - width(surface, text)) / 2, area.y(), area.height(), argb);
    }

    /** Ending at {@code right}, for a purse in a panel's corner. */
    public static void drawRight(DrawSurface surface, String text, int right, int y, int height, int argb) {
        draw(surface, text, right - width(surface, text), y, height, argb);
    }
}
