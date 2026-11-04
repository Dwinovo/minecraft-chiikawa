package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.widget.Chip;
import com.dwinovo.chiikawa.ui.widget.Segments;
import com.dwinovo.chiikawa.ui.widget.Switch;
import com.dwinovo.chiikawa.ui.widget.TabStrip;

/**
 * Where everything on the pet screen is, and the room the words on it have, which the screen is laid out by and the labels
 * of every language are measured against, see {@code PetScreenLabelsTest}. The panel is as big on both of its pages and its
 * top edge never moves between them, so the tabs and the pet stay where the hand left them. The pet's picture rises over the
 * panel's top edge, so what has to fit the window is the panel and that, see {@code ScreenFitTest}. Every position is from
 * the panel's top-left corner. Nothing here needs the game, so a test can ask.
 *
 * <p>Where the slots are is the menu's to say, {@link PetBackpackMenu}; the card the pet's own are in and the lines on it are
 * here, and drawn round them.
 */
public final class PetScreenLayout {
    public static final int PANEL_W = 300;
    public static final int PANEL_H = 184;
    /** How far the pet's picture rises above the panel's top edge: more than the tabs do. */
    public static final int RISE = 24;

    // ---- the banner ---------------------------------------------------------------

    /** How tall the banner is, its line along the bottom included: the page starts under it. */
    public static final int BANNER_H = 70;
    /** Where the banner's contents end on the right. */
    public static final int RIGHT = 290;
    /** Where the pet's name, its job and its licence start. */
    public static final int TEXT_X = 94;
    /** The top of the banner's first row: the name, the chips and the badges. */
    public static final int ROW_Y = 7;
    public static final int HEARTS_Y = 30;
    /** The pet's picture: a box over the banner's left end and the air above it, and what a click in it is not a drop of. */
    public static final Rect MODEL = new Rect(11, -RISE, 73, RISE + 67);
    /** Where the pet stands, and how big it is: the tallest of the pets, ears and all, still fits under {@link #RISE}. */
    public static final int FLOOR_Y = 65;
    public static final int MODEL_SCALE = 76;
    /** The faint shadow the pet stands in. */
    public static final Rect SHADOW = new Rect(23, FLOOR_Y - 2, 48, 5);
    /** The pet's three orders, as one control under the money. */
    public static final Rect ORDERS = new Rect(RIGHT - 112, 27, 112, Segments.HEIGHT);
    public static final int BUBBLE_X = 96;
    public static final int BUBBLE_Y = 47;
    public static final int BUBBLE_H = Chip.HEIGHT;
    /** How wide the badge for a present is, and so how much the bubble gives up to it. */
    public static final int GIFT_W = 56;
    public static final int GIFT_Y = BUBBLE_Y;

    /** @return where the bubble goes: the whole of the banner's width, or less the present's badge when there is one */
    public static Rect bubble(boolean gift) {
        int right = gift ? RIGHT - GIFT_W - UiStyle.GAP : RIGHT;
        return new Rect(BUBBLE_X, BUBBLE_Y, right - BUBBLE_X, BUBBLE_H);
    }

    // ---- what the words may take --------------------------------------------------

    /** How wide a pet's name may be at the size of the rest of the font: a longer name is drawn smaller or cut. */
    public static final int NAME_ROOM = 64;
    /** The words on the job's chip and the licence's. */
    public static final int JOB_ROOM = 44;
    public static final int RANK_ROOM = 32;
    /** The words on the badge for a present. */
    public static final int GIFT_ROOM = GIFT_W - Chip.overhead();
    /** The longest count of a slip, "180/300". */
    public static final int COUNT_ROOM = 42;
    /** What a pet is at, beside the slip's picture, its bar and its count in the whole bubble. */
    public static final int ACTIVITY_ROOM = bubble(false).width() - 2 * SpeechRow.AIR
        - (SpeechRow.AIR + SpeechRow.ICON + SpeechRow.AIR + SpeechRow.LEAST_BAR + SpeechRow.AIR + COUNT_ROOM);
    /** What it says it is short of, beside its picture in the bubble that has a present beside it. */
    public static final int REASON_ROOM = bubble(true).width() - 2 * SpeechRow.AIR - SpeechRow.AIR - SpeechRow.ICON;
    /** The names of the orders in the control, which the cells of are as wide as their words. */
    public static final int ORDERS_ROOM = ORDERS.width();
    /** The tabs are not to reach back over the pet. */
    public static final int TABS_ROOM = PANEL_W - TabStrip.INSET - (MODEL.right() + UiStyle.GAP_SECTION);

    // ---- the backpack page --------------------------------------------------------

    /** The card the pet's own slots are in, the hand and the fifteen, then a line across it, then the bag and the bag's ten. */
    public static final Rect PET_CARD = new Rect(4, 74, 120, 102);
    public static final int CARD_DIVIDER_Y = 133;

    // ---- the settings page --------------------------------------------------------

    /** One card as tall as the backpack's, with the one row in it. */
    public static final Rect SETTINGS_CARD = new Rect(4, PET_CARD.y(), 286, PET_CARD.height());
    public static final int ROW_H = SETTINGS_CARD.height() - 2;
    public static final int FIRST_ROW_Y = SETTINGS_CARD.y() + 1;
    /** Where a row's picture is, and its words: the title, and under it the line that says what it does. */
    public static final int ROW_ICON_X = SETTINGS_CARD.x() + UiStyle.PAD;
    public static final int ROW_TEXT_X = ROW_ICON_X + UiStyle.ICON + UiStyle.GAP;
    /** Where a row's control ends. */
    public static final int ROW_CONTROL_RIGHT = SETTINGS_CARD.right() - UiStyle.PAD;
    /** How wide a line of the words of a row may be: the room up to its control, and the air before that. */
    public static final int SWITCH_ROW_ROOM = ROW_CONTROL_RIGHT - Switch.WIDTH - UiStyle.PAD - ROW_TEXT_X;

    private PetScreenLayout() {
    }

    /** @return the x of the panel's left edge in a window this wide: in the middle of it */
    public static int left(int windowWidth) {
        return (windowWidth - PANEL_W) / 2;
    }

    /** @return the y of the panel's top edge in a window this high: the panel and the pet over it, in the middle of it */
    public static int top(int windowHeight) {
        return (windowHeight - PANEL_H - RISE) / 2 + RISE;
    }
}
