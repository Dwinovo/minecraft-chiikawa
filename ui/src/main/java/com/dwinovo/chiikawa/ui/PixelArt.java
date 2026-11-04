package com.dwinovo.chiikawa.ui;

/**
 * A small picture drawn from a grid of letters, one letter a pixel, in the theme's own
 * colours: the mod's icons for things the game has no item for, drawn in the manga's ink
 * line so they sit with everything else on a panel.
 *
 * <p>In code rather than in a texture for the same reason the panels are: a letter grid is
 * read and edited where it is used, and it cannot fall out of step with the palette.
 *
 * <h2>The letters</h2>
 * {@code X} ink, {@code w} paper, {@code o} wood, {@code l} light wood, {@code p} pink, {@code g} leaf,
 * {@code s} shade; anything else is left clear.
 */
public record PixelArt(String... rows) {
    /** A wrapped box with a pink ribbon: a present. */
    public static final PixelArt GIFT = new PixelArt(
        "..XXX...XXX..",
        ".XpppX.XpppX.",
        ".XpppXXXpppX.",
        "XXXXXXXXXXXXX",
        "XwwwwwpwwwwwX",
        "XXXXXXpXXXXXX",
        "XwwwwwpwwwwwX",
        "XwwwwwpwwwwwX",
        "XwwwwwpwwwwwX",
        "XwwwwwpwwwwwX",
        ".XXXXXXXXXXX.");

    /** A basket with a leaf and a berry in it: picking things up. */
    public static final PixelArt PICK_UP = new PixelArt(
        "....XXXXXX....",
        "...X.gg.ppX...",
        ".XXXXXXXXXXXX.",
        "XlolololololoX",
        "XololololololX",
        "XlolololololoX",
        ".XololololoX..",
        "..XlolololoX..",
        "...XXXXXXXX...");

    /** A cog in wood: what can be set. */
    public static final PixelArt GEAR = new PixelArt(
        ".....XXXX.....",
        ".XX..XooX..XX.",
        ".XoXXXooXXXoX.",
        ".XooooooooooX.",
        "..XoooXXoooX..",
        "XXXooX..XooXXX",
        "XooooX..XooooX",
        "XooooX..XooooX",
        "XXXooX..XooXXX",
        "..XoooXXoooX..",
        ".XooooooooooX.",
        ".XoXXXooXXXoX.",
        ".XX..XooX..XX.",
        ".....XXXX.....");

    /** A bag, as a shadow in the shade of an empty well: where a bag would be worn. */
    public static final PixelArt BAG = new PixelArt(
        "....ssss....",
        "...s....s...",
        "..ssssssss..",
        ".ssssssssss.",
        ".ssssssssss.",
        ".ss.ssss.ss.",
        ".ss.ssss.ss.",
        ".ssssssssss.",
        ".ssssssssss.",
        "..ssssssss..");

    public int width() {
        int widest = 0;
        for (String row : rows) {
            widest = Math.max(widest, row.length());
        }
        return widest;
    }

    public int height() {
        return rows.length;
    }

    /** Draws it with its top-left corner at x, y: one rectangle per run of a colour. */
    public void draw(DrawSurface surface, int x, int y) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int start = 0;
            while (start < line.length()) {
                char c = line.charAt(start);
                int end = start + 1;
                while (end < line.length() && line.charAt(end) == c) {
                    end++;
                }
                int argb = colour(c);
                if (argb != 0) {
                    surface.fillRect(x + start, y + row, end - start, 1, argb);
                }
                start = end;
            }
        }
    }

    /** Draws it in the middle of a square {@code box} wide, where an icon would go. */
    public void drawCentered(DrawSurface surface, int x, int y, int box) {
        draw(surface, x + (box - width()) / 2, y + (box - height()) / 2);
    }

    /** @return the colour a letter stands for, or 0 for clear */
    static int colour(char c) {
        return switch (c) {
            case 'X' -> UiTheme.INK;
            case 'w' -> UiTheme.PANEL;
            case 'o' -> UiTheme.WOOD;
            case 'l' -> UiTheme.WOOD_LIGHT;
            case 'p' -> UiTheme.ACCENT_SOFT;
            case 'g' -> UiTheme.LEAF;
            case 's' -> UiTheme.SHADE;
            default -> 0;
        };
    }
}
