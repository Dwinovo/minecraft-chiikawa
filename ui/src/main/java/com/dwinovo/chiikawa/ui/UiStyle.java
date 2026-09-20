package com.dwinovo.chiikawa.ui;

/**
 * The mod's spacing and sizes. Colour lives in {@link UiTheme}; everything geometric
 * lives here, so tightening the layout is one file.
 *
 * <p>Only values more than one screen uses belong here. A screen's own column widths stay
 * in that screen — moving those here would just be a different kind of scattering.
 */
public final class UiStyle {
    /** Panel outline width. */
    public static final int BORDER = 1;
    /** Space between a panel's edge and its contents. */
    public static final int PAD = 8;
    /** One text line plus its leading. */
    public static final int LINE = 10;
    /** Height of a panel's title bar. */
    public static final int TITLE_H = 20;
    /** Vertical step between two entries of a list. */
    public static final int ROW_PITCH = 22;
    /** Space a nested line is indented by. */
    public static final int INDENT = 8;

    private UiStyle() {
    }
}
