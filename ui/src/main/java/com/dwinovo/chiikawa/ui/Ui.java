package com.dwinovo.chiikawa.ui;

/**
 * The surfaces a screen is built on: the sticker a panel is, the well cut into it, the
 * line between two of its parts. Things that go <i>on</i> a panel — a bar, a badge, an
 * icon in its slot — live in {@link com.dwinovo.chiikawa.ui.widget}.
 *
 * <p>All of it drawn, none of it a texture, so a screen is laid out in code and stays in
 * step with {@link UiTheme} and {@link UiStyle}.
 *
 * <h2>Every panel is a sticker</h2>
 * An ink line, a white rim just inside it, and paper: the way Chiikawa Pocket cuts every
 * card, and the way the manga draws everything. The rounding is real — a curve worked out
 * row by row, not a missing corner pixel — because at this size a corner is most of what
 * tells a friendly shape from a spreadsheet cell.
 */
public final class Ui {
    /** How round a panel is. */
    public static final int PANEL_RADIUS = 5;
    /** How round something small on a panel is: a chip, a card inside a card. */
    public static final int CARD_RADIUS = 4;
    /** How round a well is: just enough to soften it. */
    public static final int WELL_RADIUS = 2;
    /** A dash of a divider, and the gap after it. */
    private static final int DASH = 2;

    private Ui() {
    }

    /**
     * A filled rounded box, built from horizontal spans: the rows that curve in at the top
     * and bottom, and one block between them.
     */
    public static void roundRect(DrawSurface surface, int x, int y, int width, int height, int radius, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        for (int row = 0; row < r; row++) {
            int inset = inset(r, row);
            surface.fillRect(x + inset, y + row, width - 2 * inset, 1, argb);
            surface.fillRect(x + inset, y + height - 1 - row, width - 2 * inset, 1, argb);
        }
        surface.fillRect(x, y + r, width, height - 2 * r, argb);
    }

    /**
     * How far in the curve is on a given row of a corner: the first column whose pixel
     * centre lies inside the circle. The third of a pixel of slack is what keeps a small
     * radius from reading as a chamfer.
     */
    static int inset(int radius, int row) {
        double reach = (radius + 0.3) * (radius + 0.3) - (double) (radius - row) * (radius - row);
        for (int column = 0; column < radius; column++) {
            if ((double) (radius - column) * (radius - column) <= reach) {
                return column;
            }
        }
        return radius;
    }

    /** A sticker: the ink line, the white rim inside it, and whatever it is filled with. */
    public static void sticker(DrawSurface surface, int x, int y, int width, int height, int radius, int fill) {
        roundRect(surface, x, y, width, height, radius, UiTheme.INK);
        roundRect(surface, x + 1, y + 1, width - 2, height - 2, Math.max(0, radius - 1), UiTheme.HIGHLIGHT);
        roundRect(surface, x + 2, y + 2, width - 4, height - 4, Math.max(0, radius - 2), fill);
    }

    /** A panel: the sticker in paper. */
    public static void panel(DrawSurface surface, int x, int y, int width, int height) {
        sticker(surface, x, y, width, height, PANEL_RADIUS, UiTheme.PANEL);
    }

    /**
     * A panel that stands off what is behind it, by the shadow it casts rather than by a
     * heavier outline. Depth is what tells a reader which layer they are on; a thicker
     * border only says the same thing louder.
     */
    public static void card(DrawSurface surface, int x, int y, int width, int height) {
        roundRect(surface, x + UiStyle.SHADOW_OFF, y + UiStyle.SHADOW_OFF, width, height, PANEL_RADIUS, UiTheme.SHADOW);
        panel(surface, x, y, width, height);
    }

    /**
     * A well pressed into the paper: where something is put rather than where something is
     * written. A shaded lip along the top and left and a floor a shade below the paper —
     * a dip, not a hole, which is what the paper around it can take.
     *
     * <p>No outline: a well is as wide as it is told to be, and an item slot has exactly
     * sixteen pixels inside eighteen. A line would have to come out of the item.
     */
    public static void well(DrawSurface surface, int x, int y, int width, int height) {
        roundRect(surface, x, y, width, height, WELL_RADIUS, UiTheme.SHADE);
        roundRect(surface, x + 1, y + 1, width - 1, height - 1, WELL_RADIUS, UiTheme.SURFACE);
    }

    /**
     * The line between two parts of a panel: dashed, and kept in from the edges. A solid
     * line across a sticker cuts it in two; a dashed one only says where one part ends.
     */
    public static void divider(DrawSurface surface, int x, int y, int width) {
        for (int at = x + UiStyle.PAD; at < x + width - UiStyle.PAD; at += 2 * DASH) {
            surface.fillRect(at, y, Math.min(DASH, x + width - UiStyle.PAD - at), 1, UiTheme.DIVIDER);
        }
    }

    /** Tints the row the cursor is on. A list that answers the mouse is a list you can use. */
    public static void rowHighlight(DrawSurface surface, Rect row) {
        roundRect(surface, row.x(), row.y(), row.width(), row.height(), CARD_RADIUS, UiTheme.HOVER);
    }

    /** Text ending at {@code right}, for counts and other trailing detail. */
    public static void textRight(DrawSurface surface, String text, int right, int y, int argb) {
        surface.drawText(text, right - surface.textWidth(text), y, argb);
    }

    /** Text in the middle of a box, across and down — what a button's face wants. */
    public static void textCentered(DrawSurface surface, String text, Rect area, int argb) {
        surface.drawText(text, area.x() + (area.width() - surface.textWidth(text)) / 2,
            UiStyle.centerIn(area.y(), area.height(), surface.lineHeight()), argb);
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
