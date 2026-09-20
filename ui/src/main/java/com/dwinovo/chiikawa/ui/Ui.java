package com.dwinovo.chiikawa.ui;

/**
 * The pieces the mod's screens are built from: a panel, a divider, a line of text. All of
 * it drawn, none of it a texture, so a screen is laid out in code and stays in step with
 * {@link UiTheme} and {@link UiStyle}.
 */
public final class Ui {
    private Ui() {
    }

    /** Panel body with a one-pixel outline. */
    public static void panel(DrawSurface surface, int x, int y, int width, int height) {
        surface.fillRect(x, y, width, height, UiTheme.BORDER);
        surface.fillRect(x + UiStyle.BORDER, y + UiStyle.BORDER,
            width - 2 * UiStyle.BORDER, height - 2 * UiStyle.BORDER, UiTheme.PANEL);
    }

    /**
     * Panel with a title above a divider.
     *
     * @return the y its contents start at
     */
    public static int titledPanel(DrawSurface surface, int x, int y, int width, int height, String title) {
        panel(surface, x, y, width, height);
        surface.drawText(title, x + UiStyle.PAD, y + (UiStyle.TITLE_H - surface.lineHeight()) / 2 + 1, UiTheme.TEXT);
        divider(surface, x, y + UiStyle.TITLE_H, width);
        return y + UiStyle.TITLE_H + UiStyle.PAD;
    }

    /** Full-width line between two sections of a panel. */
    public static void divider(DrawSurface surface, int x, int y, int width) {
        surface.fillRect(x + UiStyle.BORDER, y, width - 2 * UiStyle.BORDER, UiStyle.BORDER, UiTheme.DIVIDER);
    }

    /** Text ending at {@code right}, for counts and other trailing detail. */
    public static void textRight(DrawSurface surface, String text, int right, int y, int argb) {
        surface.drawText(text, right - surface.textWidth(text), y, argb);
    }

    /** Text from {@code x}, cut with an ellipsis at {@code maxWidth} so a long name cannot spill. */
    public static void textClipped(DrawSurface surface, String text, int x, int y, int maxWidth, int argb) {
        surface.drawText(TextClip.clip(text, maxWidth, surface::textWidth), x, y, argb);
    }
}
