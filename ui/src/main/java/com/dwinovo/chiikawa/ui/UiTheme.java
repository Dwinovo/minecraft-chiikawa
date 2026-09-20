package com.dwinovo.chiikawa.ui;

/**
 * The mod's colours, written down as what a colour is <i>for</i> rather than what it looks
 * like. A widget asks for {@link #TEXT_MUTED}, never for a brown, so the day the palette
 * changes it changes here and nowhere else.
 *
 * <h2>What earns a slot</h2>
 * A slot is a job on screen that more than one widget has. "The colour of the labor
 * board's second line" is not a job; "words that support other words" is. Slots that
 * multiply until each widget has its own are just hard-coded colours with longer names.
 */
public final class UiTheme {
    /** A card's face: what content sits on. */
    public static final int PANEL = 0xFFF7E9D2;
    /** A well cut into a card — an icon's box, a bar's groove. Reads as recessed. */
    public static final int SURFACE = 0xFFE7D2B2;
    /** The lit edge of anything raised, and the far edge of anything sunken. */
    public static final int HIGHLIGHT = 0xFFFFF7E8;
    /** The shaded edge of anything raised, and the near edge of anything sunken. */
    public static final int SHADE = 0xFFD9BE96;
    /** The line around a card. */
    public static final int BORDER = 0xFFA9754F;
    /** The line between two parts of one card; quieter than a border, which encloses. */
    public static final int DIVIDER = 0xFFD9C3A5;
    /** Words. */
    public static final int TEXT = 0xFF5A3A2B;
    /** Words that support other words: units, hints, who took what. */
    public static final int TEXT_MUTED = 0xFF8A7360;
    /** Work under way: the filled part of a bar. */
    public static final int ACCENT = 0xFFD08A3E;
    /** Finished, taken, paid. */
    public static final int SUCCESS = 0xFF4C7A34;
    /** A life still in hand. Lives lost are drawn in {@link #SHADE}: the row keeps its length. */
    public static final int LIFE = 0xFFE0748A;
    /** Laid over the row the cursor is on, so a list answers the mouse. */
    public static final int HOVER = 0x33A9754F;
    /** The hard shadow a card casts, one step down and to the right. */
    public static final int SHADOW = 0x40402A1E;

    private UiTheme() {
    }
}
