package com.dwinovo.chiikawa.ui;

/**
 * The mod's colours, named by what they mean rather than what they are. Screens name a
 * slot here and never write a colour of their own, so the whole look changes from this
 * one file.
 *
 * <p>Cream and brown, the palette of the pets' own art.
 */
public final class UiTheme {
    /** Panel body. */
    public static final int PANEL = 0xFFF7E9D2;
    /** Panel outline, one pixel. */
    public static final int BORDER = 0xFFA9754F;
    /** Line between a panel's sections. */
    public static final int DIVIDER = 0xFFD9C3A5;
    /** Headings and anything the player reads first. */
    public static final int TEXT = 0xFF5A3A2B;
    /** Supporting text: units, jobs, "nobody took it yet". */
    public static final int TEXT_MUTED = 0xFF8A7360;
    /** Something went well: a slip taken, work finished. */
    public static final int SUCCESS = 0xFF4C7A34;
    /** A label floating in the world carries its own backdrop. */
    public static final int LABEL_BACKDROP = 0xE6F7E9D2;

    private UiTheme() {
    }
}
