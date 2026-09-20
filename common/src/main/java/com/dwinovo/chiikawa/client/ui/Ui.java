package com.dwinovo.chiikawa.client.ui;

import net.minecraft.network.chat.Component;

/**
 * The pieces every screen of the mod is built from: a panel, a divider, a line of text.
 * All of it drawn, none of it a texture, so a screen is laid out in code and stays in
 * step with {@link PetTheme} and {@link PetStyle}.
 */
public final class Ui {
    private Ui() {
    }

    /** Panel body with a one-pixel outline. */
    public static void panel(Surface surface, int x, int y, int width, int height) {
        surface.fillRect(x, y, width, height, PetTheme.BORDER);
        surface.fillRect(x + PetStyle.BORDER, y + PetStyle.BORDER,
            width - 2 * PetStyle.BORDER, height - 2 * PetStyle.BORDER, PetTheme.PANEL);
    }

    /** Panel with a title above a divider; returns the y the contents start at. */
    public static int titledPanel(Surface surface, int x, int y, int width, int height, Component title) {
        panel(surface, x, y, width, height);
        surface.drawText(title, x + PetStyle.PAD, y + (PetStyle.TITLE_H - surface.lineHeight()) / 2 + 1, PetTheme.TEXT);
        divider(surface, x, y + PetStyle.TITLE_H, width);
        return y + PetStyle.TITLE_H + PetStyle.PAD;
    }

    /** Full-width line between two sections of a panel. */
    public static void divider(Surface surface, int x, int y, int width) {
        surface.fillRect(x + PetStyle.BORDER, y, width - 2 * PetStyle.BORDER, PetStyle.BORDER, PetTheme.DIVIDER);
    }

    /** Text against the right edge of {@code width}, for counts and jobs. */
    public static void textRight(Surface surface, Component text, int right, int y, int argb) {
        surface.drawText(text, right - surface.textWidth(text), y, argb);
    }
}
