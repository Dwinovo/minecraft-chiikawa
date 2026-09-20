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

    /** Panel face with a one-pixel outline. */
    public static void panel(DrawSurface surface, int x, int y, int width, int height) {
        surface.fillRect(x, y, width, height, UiTheme.BORDER);
        surface.fillRect(x + UiStyle.BORDER, y + UiStyle.BORDER,
            width - 2 * UiStyle.BORDER, height - 2 * UiStyle.BORDER, UiTheme.PANEL);
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
     * written. Darker than the face, so it reads as below it.
     */
    public static void well(DrawSurface surface, int x, int y, int width, int height) {
        surface.fillRect(x, y, width, height, UiTheme.DIVIDER);
        surface.fillRect(x + UiStyle.BORDER, y + UiStyle.BORDER,
            width - 2 * UiStyle.BORDER, height - 2 * UiStyle.BORDER, UiTheme.SURFACE);
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
