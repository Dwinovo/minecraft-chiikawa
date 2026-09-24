package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * What a pet says, in a speech bubble: a small card with a tail pointing down at whoever
 * is talking. The pets in the handbook talk in it, and so do the pets in the world, so a
 * line reads the same wherever it is said.
 *
 * @param text the words
 */
public record Bubble(String text) {
    /** How far the tail hangs below the card. */
    public static final int TAIL = 2;
    /** How near the card's corners the tail may go before it runs into their curve. */
    private static final int TAIL_INSET = 3;

    public int width(DrawSurface surface) {
        return surface.textWidth(text) + 2 * UiStyle.GAP;
    }

    /** The card's height, the tail under it not counted. */
    public int height(DrawSurface surface) {
        return surface.lineHeight() + 2 * UiStyle.TIGHT;
    }

    /**
     * Draws the card with its top-left corner at x, y, and the tail under it at
     * {@code tailX} — or as near to it as the card reaches.
     */
    public void draw(DrawSurface surface, int x, int y, int tailX) {
        int width = width(surface);
        int height = height(surface);
        Ui.sticker(surface, x, y, width, height, Ui.CARD_RADIUS, UiTheme.PANEL);
        int tail = Math.max(x + TAIL_INSET, Math.min(tailX, x + width - TAIL_INSET - 1));
        surface.fillRect(tail - 1, y + height, 3, 1, UiTheme.INK);
        surface.fillRect(tail, y + height + 1, 1, 1, UiTheme.INK);
        surface.drawText(text, x + UiStyle.GAP, y + UiStyle.TIGHT, UiTheme.TEXT);
    }
}
