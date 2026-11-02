package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * How tall the shop's counter is, which the screen lays itself out by and {@code ScreenFitTest} asks of: a page of rows, then
 * the page arrows. Nothing here needs the game, so a test can ask.
 */
public final class ShopLayout {
    /** How many rows a page has, which is as many as the smallest window has the room for. */
    public static final int ROWS = 7;
    public static final int ROW_H = UiStyle.ROW_H;

    private ShopLayout() {
    }

    /** @return how tall the panel is: a page of rows, full or not, and the footer under it */
    public static int panelHeight() {
        return UiStyle.TITLE_H + UiStyle.PAD + ROWS * ROW_H + UiStyle.PAD + UiStyle.CONTROL_H + UiStyle.PAD;
    }
}
