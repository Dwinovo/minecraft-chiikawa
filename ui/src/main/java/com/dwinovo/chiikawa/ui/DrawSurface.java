package com.dwinovo.chiikawa.ui;

/**
 * Everything this library can do to a screen. Minecraft is on the other side of these few
 * calls and nowhere else in the module, so the widgets can be built and tested without it.
 */
public interface DrawSurface {
    void fillRect(int x, int y, int width, int height, int argb);

    void drawText(String text, int x, int y, int argb);

    /** Draws an icon in an {@link UiStyle#ICON} box with its top-left corner at x, y. */
    default void drawIcon(Icon icon, int x, int y) {
        drawIcon(icon, x, y, UiStyle.ICON);
    }

    /**
     * Draws an icon shrunk to a box {@code size} wide, for a small sticker that has no room for a whole item: the same
     * picture, only smaller.
     */
    void drawIcon(Icon icon, int x, int y, int size);

    int textWidth(String text);

    int lineHeight();
}
