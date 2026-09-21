package com.dwinovo.chiikawa.ui;

/**
 * The mod's colours, by what they are for.
 *
 * <p>Drawn after Chiikawa Pocket, the official game, rather than after the vanilla GUI:
 * <b>cream paper and a dark ink line</b>, a white rim just inside the line so every panel
 * reads as a sticker, wood for anything written on a sign, and colour kept for things that
 * mean something — pink for what is picked, green for work and how far along it is, sky
 * behind the pet. The old palette put beige, tan and brown in one hue and one value, so
 * nothing stood off anything; this one keeps the paper light, the ink dark, and the colour
 * saturated only where it has a job.
 */
public final class UiTheme {
    /** The paper every panel is cut from. */
    public static final int PANEL = 0xFFFFF9EF;
    /** The line round everything: the manga's own ink, a warm near-black. */
    public static final int INK = 0xFF4B3526;
    /** Kept for callers that ask for "the outline": it is the ink. */
    public static final int BORDER = INK;
    /** The white rim just inside the ink, which is what makes a panel read as a sticker. */
    public static final int HIGHLIGHT = 0xFFFFFFFF;
    /** The floor of a well: where something is put, a shade below the paper. */
    public static final int SURFACE = 0xFFF3E6D2;
    /** A well's shadowed lip, an empty heart, the dashes of a divider. */
    public static final int SHADE = 0xFFDDC8A8;
    /** Between two parts of a panel: dashed, in the lip's colour. */
    public static final int DIVIDER = SHADE;
    /** Text: the ink again, so words and lines are one colour. */
    public static final int TEXT = INK;
    /** What can be read second. */
    public static final int TEXT_MUTED = 0xFFA48B74;
    /** Pink: what is picked, and the one thing on a screen to press. */
    public static final int ACCENT = 0xFFF49AB1;
    /** A picked card's fill: the pink, softened so ink reads on it. */
    public static final int ACCENT_SOFT = 0xFFF9C3D0;
    /** Behind something the pink is about. */
    public static final int ACCENT_PALE = 0xFFFDE4EB;
    /** Behind the pet: its own bit of sky. */
    public static final int SKY_PALE = 0xFFDDF1FA;
    /** Work, and how far along it is. */
    public static final int LEAF = 0xFFA5D48C;
    /** Behind something the green is about. */
    public static final int LEAF_PALE = 0xFFE6F4DC;
    /** Done. */
    public static final int SUCCESS = 0xFF6DA356;
    /** Hearts. */
    public static final int LIFE = ACCENT;
    /** Money: the emerald everything is priced in. */
    public static final int EMERALD = 0xFF3FBF6E;
    /** The lit facet of an emerald. */
    public static final int EMERALD_LIGHT = 0xFFA8EDC0;
    /** A sign's board — the labor board's own wood. */
    public static final int WOOD = 0xFFE4BE8C;
    /** The lit edge of a board. */
    public static final int WOOD_LIGHT = 0xFFF0D3A9;
    /** The grain, and the board's shaded edge. */
    public static final int WOOD_DARK = 0xFFB3844F;
    /** Laid over a row the cursor is on. */
    public static final int HOVER = 0x33F49AB1;
    /** Under a card, so it stands off what is behind it. */
    public static final int SHADOW = 0x46140E08;

    private UiTheme() {
    }

    /** A colour washed most of the way to white, for a pale fill behind that colour. */
    public static int pale(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        r = r + (255 - r) * 3 / 4;
        g = g + (255 - g) * 3 / 4;
        b = b + (255 - b) * 3 / 4;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
