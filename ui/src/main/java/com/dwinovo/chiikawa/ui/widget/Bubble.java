package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * What a pet says, in a speech bubble: a small card with a tail pointing down at whoever
 * is talking. The pets in the handbook talk in it, and so do the pets in the world, so a
 * line reads the same wherever it is said. A screen that has more than a line to put in
 * one asks for the {@linkplain #frame card with its tail at the side} and fills it itself.
 *
 * @param text the words
 */
public record Bubble(String text) {
    /** How far the tail hangs below the card. */
    public static final int TAIL = 2;
    /** How far the side tail of a {@link #frame} reaches out of the card. */
    public static final int SIDE_TAIL = 3;
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

    /**
     * A card with its tail out of the left side, pointing at a speaker beside it, and nothing in it: for a bubble that says
     * more than a line, which the caller lays out inside {@code area}. The tail is where the card's side is, as near to
     * {@code tailY} as the card's corners let it be.
     */
    public static void frame(DrawSurface surface, Rect area, int tailY) {
        Ui.sticker(surface, area.x(), area.y(), area.width(), area.height(), Ui.CARD_RADIUS, UiTheme.PANEL);
        int tip = Math.max(area.y() + TAIL_INSET + 1, Math.min(tailY, area.bottom() - TAIL_INSET - 2));
        int x = area.x();
        // The tail is a small triangle, open to the card: paper inside it, the ink line round it.
        surface.fillRect(x - SIDE_TAIL + 1, tip, SIDE_TAIL + 1, 1, UiTheme.PANEL);
        surface.fillRect(x - SIDE_TAIL + 2, tip - 1, SIDE_TAIL, 3, UiTheme.PANEL);
        surface.fillRect(x - SIDE_TAIL, tip, 1, 1, UiTheme.INK);
        surface.fillRect(x - SIDE_TAIL + 1, tip - 1, 1, 1, UiTheme.INK);
        surface.fillRect(x - SIDE_TAIL + 1, tip + 1, 1, 1, UiTheme.INK);
        surface.fillRect(x - SIDE_TAIL + 2, tip - 2, 1, 1, UiTheme.INK);
        surface.fillRect(x - SIDE_TAIL + 2, tip + 2, 1, 1, UiTheme.INK);
    }
}
