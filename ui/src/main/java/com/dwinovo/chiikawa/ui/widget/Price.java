package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * Words that end in an amount of money — "Buy 3", "Upgrade 20", or only the number — with
 * the money itself pictured after them, so a number on a button or in a corner reads as a
 * price rather than as a count of the thing beside it. What money is belongs to the game:
 * the caller hands over its picture.
 */
public final class Price {
    private Price() {
    }

    /** What {@link #draw} takes up for words {@code textWidth} wide, for laying out before drawing. */
    public static int width(int textWidth) {
        return textWidth + UiStyle.TIGHT + UiStyle.ICON;
    }

    public static int width(DrawSurface surface, String text) {
        return width(surface.textWidth(text));
    }

    /**
     * Draws it from {@code x}, the words and the money each in the middle of a box
     * {@code height} tall from {@code y}.
     *
     * @param coin the money's picture
     * @return the width it took
     */
    public static int draw(DrawSurface surface, Icon coin, String text, int x, int y, int height, int argb) {
        surface.drawText(text, x, UiStyle.centerIn(y, height, surface.lineHeight()), argb);
        surface.drawIcon(coin, x + surface.textWidth(text) + UiStyle.TIGHT, UiStyle.centerIn(y, height, UiStyle.ICON));
        return width(surface, text);
    }

    /** In the middle of a box — what a button's face wants. */
    public static void drawCentered(DrawSurface surface, Icon coin, String text, Rect area, int argb) {
        draw(surface, coin, text, area.x() + (area.width() - width(surface, text)) / 2, area.y(), area.height(), argb);
    }

    /** Ending at {@code right}, for a purse in a panel's corner. */
    public static void drawRight(DrawSurface surface, Icon coin, String text, int right, int y, int height, int argb) {
        draw(surface, coin, text, right - width(surface, text), y, height, argb);
    }
}
