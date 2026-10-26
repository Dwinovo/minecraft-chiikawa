package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * The room the words on the pet screen's status page have under the job and its licences: what the pet would do next and
 * why it is not at work, a card each with a picture to the left, which the screen draws them in and the words of every
 * language are measured against, see {@code StatusLabelsTest}. Nothing here needs the game, so a test can ask.
 */
public final class StatusLayout {
    /** A card: the panel's width inside its padding. */
    public static final int CARD_W = OrdersLayout.CARD_W;
    /** The box the picture on a card sits in, which none of them is wider than. */
    public static final int ROW_ICON_W = 14;
    /** Where the words start in a card. */
    public static final int ROW_TEXT_X = UiStyle.GAP + ROW_ICON_W + UiStyle.GAP;
    /** How wide a line of the words may be. */
    public static final int ROW_TEXT_W = CARD_W - ROW_TEXT_X - UiStyle.GAP;
    /** How many lines the words may take: a longer one is cut with an ellipsis. */
    public static final int ROW_LINES = 2;
    /** How tall a card is. */
    public static final int ROW_H = ROW_LINES * UiStyle.LINE + 2 * UiStyle.TIGHT;
    /** How tall the two cards are together, with the gap between them. */
    public static final int OUTLOOK_H = 2 * ROW_H + UiStyle.GAP;

    private StatusLayout() {
    }
}
