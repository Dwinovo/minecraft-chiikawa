package com.dwinovo.chiikawa.ui;

/**
 * The mod's spacing and sizes. Colour lives in {@link UiTheme}; everything geometric lives
 * here, so tightening the layout is one file.
 *
 * <h2>The scale</h2>
 * Space comes from {@link #TIGHT}, {@link #GAP}, {@link #PAD} and {@link #GAP_SECTION} and
 * nowhere else. A layout built from four values reads as deliberate; one built from
 * whatever number looked right reads as nearly-aligned, which is the look of a thing
 * nobody laid out. The steps grow by half again each time, far enough apart that the eye
 * can tell "these belong together" from "these are two groups" — which is all grouping is.
 *
 * <h2>What earns a constant</h2>
 * Only a value more than one screen uses. A screen's own column widths stay in that
 * screen; moving those here is a different kind of scattering, not less of it.
 */
public final class UiStyle {
    /** Outline width. One pixel: at this size, anything thicker eats the content. */
    public static final int BORDER = 1;

    /** Inside one thing: an icon and its well, a number and its bar. */
    public static final int TIGHT = 2;
    /** Between things that belong together: an icon and the name beside it. */
    public static final int GAP = 4;
    /** A container's own breathing room, and the step between two groups in it. */
    public static final int PAD = 8;
    /** Between parts of a card that are about different things. */
    public static final int GAP_SECTION = 12;

    /** One text line plus its leading. */
    public static final int LINE = 10;
    /** Height of a card's title bar. */
    public static final int TITLE_H = 20;
    /** An icon's box — the size the game draws an item at, never scaled. */
    public static final int ICON = 16;
    /** An icon's well, the icon plus its frame. */
    public static final int SLOT = ICON + 2 * BORDER;
    /** A list row: a slot with air above and below it. */
    public static final int ROW_H = SLOT + 2 * TIGHT;
    /** Step from one row to the next: the row, plus the air between two of them. */
    public static final int ROW_PITCH = ROW_H + GAP;
    /** A bar's thickness. Thin enough to be a reading, thick enough to see at a glance. */
    public static final int BAR_H = 4;
    /** Space above and below the line inside a chip. */
    public static final int CHIP_PAD = 3;
    /** How far a card's shadow falls. */
    public static final int SHADOW_OFF = 2;

    private UiStyle() {
    }

    /** The top edge that puts something {@code itemHeight} tall in the middle of a row. */
    public static int centerIn(int y, int height, int itemHeight) {
        return y + (height - itemHeight) / 2;
    }
}
