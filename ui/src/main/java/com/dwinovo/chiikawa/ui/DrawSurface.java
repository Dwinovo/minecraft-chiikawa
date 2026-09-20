package com.dwinovo.chiikawa.ui;

/**
 * Everything this library can do to a screen. Minecraft is on the other side of these four
 * calls and nowhere else in the module, so the widgets can be built and tested without it.
 */
public interface DrawSurface {
    void fillRect(int x, int y, int width, int height, int argb);

    void drawText(String text, int x, int y, int argb);

    /** Draws an icon in an {@link UiStyle#ICON} box with its top-left corner at x, y. */
    void drawIcon(Icon icon, int x, int y);

    int textWidth(String text);

    int lineHeight();
}
