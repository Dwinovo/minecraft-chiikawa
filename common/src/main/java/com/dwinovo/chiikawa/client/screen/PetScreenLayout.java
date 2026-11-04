package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.widget.Tabs;

/**
 * How tall the pet screen is on each of its pages, which the screen lays itself out by and {@code ScreenFitTest} asks of. The
 * panel's top edge is placed for the tallest page and never moves between pages, so the tabs above it stay where the hand left
 * them. Nothing here needs the game, so a test can ask.
 */
public final class PetScreenLayout {
    /** The backpack page: the pet's things and the player's under them. */
    public static final int BACKPACK_HEIGHT = 214;
    /** The orders page: three orders, then whether the pet picks things up and how it fights. */
    public static final int ORDERS_HEIGHT = 189;
    /** The tallest page, which the panel's top edge is placed for. */
    public static final int TALLEST = Math.max(BACKPACK_HEIGHT, ORDERS_HEIGHT);
    /** Where a page's contents start. */
    public static final int CONTENT_Y = 24;
    /** The pet's picture, on the backpack page and the status page. */
    public static final Rect PORTRAIT = new Rect(UiStyle.PAD, CONTENT_Y, 56, 64);
    /** Status page: how tall a chip is. */
    public static final int CHIP_H = 20;
    /** Status page: the present the pet is carrying, and the air round it. */
    private static final int GIFT_ROW = 36;

    private PetScreenLayout() {
    }

    /**
     * @param gift whether the pet is carrying a present for its owner
     * @return how tall the status page is: the picture and what is beside it, the present when there is one, the job chip under
     *         that, and the two cards of what the pet would do next and why it is not at work
     */
    public static int statusHeight(boolean gift) {
        return PORTRAIT.bottom() + 6 + (gift ? GIFT_ROW : 0) + CHIP_H + UiStyle.GAP + StatusLayout.OUTLOOK_H + UiStyle.PAD;
    }

    /** @return the y of the panel's top edge in a window this high: the tabs over the tallest page in the middle of it */
    public static int top(int windowHeight) {
        return (windowHeight - TALLEST + Tabs.HEIGHT) / 2;
    }
}
