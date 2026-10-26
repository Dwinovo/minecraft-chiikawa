package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * The room the words on the pet screen's orders page have, which the screen draws them in and the labels of every
 * language are measured against, see {@code OrdersLabelsTest}. Nothing here needs the game, so a test can ask.
 */
public final class OrdersLayout {
    /** The pet screen is as wide on every page. */
    public static final int PANEL_WIDTH = 192;
    /** An order's card and the weapon swap's: the panel's width inside its padding. */
    public static final int CARD_W = PANEL_WIDTH - 2 * UiStyle.PAD;
    /** What a tick on the right of a card takes from the words. */
    private static final int TICK_W = 22;

    /** Where an order's name and its hint start in its card, to the right of its picture. */
    public static final int ORDER_TEXT_X = 32;
    /** How wide an order's name and its hint may be. */
    public static final int ORDER_TEXT_W = CARD_W - ORDER_TEXT_X - TICK_W;

    /** A stance's card: three in a row. */
    public static final int STANCE_W = (CARD_W - 2 * UiStyle.GAP) / 3;
    /** How wide a stance's name may be, centred in its card. */
    public static final int STANCE_TEXT_W = STANCE_W - 2 * UiStyle.GAP;

    /** How wide the weapon swap's name may be, from the card's left padding to its tick. */
    public static final int SWAP_TEXT_W = CARD_W - UiStyle.PAD - TICK_W;

    private OrdersLayout() {
    }
}
