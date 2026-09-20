package com.dwinovo.chiikawa.ui;

/**
 * The surfaces a screen is built on: the card, the well cut into it, the line between two
 * of its parts. Things that go <i>on</i> a card — a bar, a badge, an icon in its slot —
 * live in {@link com.dwinovo.chiikawa.ui.widget}.
 *
 * <p>All of it drawn, none of it a texture, so a screen is laid out in code and stays in
 * step with {@link UiTheme} and {@link UiStyle}.
 */
public final class Ui {
    private Ui() {
    }

    /**
     * A raised panel: outlined, with its corner pixels left out and a lit edge along the
     * top and left, a shaded one along the bottom and right.
     *
     * <p>Those two details are most of the difference between a panel and a rectangle. The
     * missing corners read as rounded at this size, and the two edges give the light a
     * direction, which is what makes a flat cream box look like a raised thing rather than
     * a hole cut in the screen. Neither costs an image.
     */
    public static void panel(DrawSurface surface, int x, int y, int width, int height) {
        int inner = UiStyle.BORDER;
        // Edges rather than one filled rectangle, so the four corner pixels stay empty.
        surface.fillRect(x + inner, y, width - 2 * inner, inner, UiTheme.BORDER);
        surface.fillRect(x + inner, y + height - inner, width - 2 * inner, inner, UiTheme.BORDER);
        surface.fillRect(x, y + inner, inner, height - 2 * inner, UiTheme.BORDER);
        surface.fillRect(x + width - inner, y + inner, inner, height - 2 * inner, UiTheme.BORDER);

        surface.fillRect(x + inner, y + inner, width - 2 * inner, height - 2 * inner, UiTheme.PANEL);
        bevel(surface, x + inner, y + inner, width - 2 * inner, height - 2 * inner,
            UiTheme.HIGHLIGHT, UiTheme.SHADE);
    }

    /**
     * A panel that stands off what is behind it, by the shadow it casts rather than by a
     * heavier outline. Depth is what tells a reader which layer they are on; a thicker
     * border only says the same thing louder.
     */
    public static void card(DrawSurface surface, int x, int y, int width, int height) {
        surface.fillRect(x + UiStyle.SHADOW_OFF, y + UiStyle.SHADOW_OFF, width, height, UiTheme.SHADOW);
        panel(surface, x, y, width, height);
    }

    /**
     * A well cut into a card: where something is put rather than where something is
     * written. The same light as a panel, turned over — shaded where a panel is lit — which
     * is the whole of what tells a hole from a lump, and the way the game draws its own
     * item slots.
     *
     * <p>No outline: a well is as wide as it is told to be, and an item slot has exactly
     * sixteen pixels inside eighteen. A border would have to come out of the item.
     */
    public static void well(DrawSurface surface, int x, int y, int width, int height) {
        surface.fillRect(x, y, width, height, UiTheme.SURFACE);
        bevel(surface, x, y, width, height, UiTheme.SHADE, UiTheme.HIGHLIGHT);
    }

    /** Lit along the top and left, shaded along the bottom and right — or the other way about. */
    private static void bevel(DrawSurface surface, int x, int y, int width, int height, int top, int bottom) {
        int edge = UiStyle.BORDER;
        surface.fillRect(x, y, width - edge, edge, top);
        surface.fillRect(x, y, edge, height - edge, top);
        surface.fillRect(x + edge, y + height - edge, width - edge, edge, bottom);
        surface.fillRect(x + width - edge, y + edge, edge, height - edge, bottom);
    }

    /**
     * Card with a title above a divider.
     *
     * @return the y its contents start at
     */
    public static int titledPanel(DrawSurface surface, int x, int y, int width, int height, String title) {
        card(surface, x, y, width, height);
        surface.drawText(title, x + UiStyle.PAD, UiStyle.centerIn(y, UiStyle.TITLE_H, surface.lineHeight()), UiTheme.TEXT);
        divider(surface, x, y + UiStyle.TITLE_H, width);
        return y + UiStyle.TITLE_H + UiStyle.PAD;
    }

    /** Full-width line between two sections of a card. */
    public static void divider(DrawSurface surface, int x, int y, int width) {
        surface.fillRect(x + UiStyle.BORDER, y, width - 2 * UiStyle.BORDER, UiStyle.BORDER, UiTheme.DIVIDER);
    }

    /** Tints the row the cursor is on. A list that answers the mouse is a list you can use. */
    public static void rowHighlight(DrawSurface surface, Rect row) {
        surface.fillRect(row.x(), row.y(), row.width(), row.height(), UiTheme.HOVER);
    }

    /** Text ending at {@code right}, for counts and other trailing detail. */
    public static void textRight(DrawSurface surface, String text, int right, int y, int argb) {
        surface.drawText(text, right - surface.textWidth(text), y, argb);
    }

    /** Text from {@code x}, cut with an ellipsis at {@code maxWidth} so a long name cannot spill. */
    public static void textClipped(DrawSurface surface, String text, int x, int y, int maxWidth, int argb) {
        surface.drawText(TextClip.clip(text, maxWidth, surface::textWidth), x, y, argb);
    }

    /**
     * The line a panel shows when it has nothing in it, centred and quiet. An empty panel
     * with nothing written in it reads as broken; one that says it is empty reads as empty.
     */
    public static void emptyState(DrawSurface surface, String text, int centerX, int y) {
        surface.drawText(text, centerX - surface.textWidth(text) / 2, y, UiTheme.TEXT_MUTED);
    }
}
