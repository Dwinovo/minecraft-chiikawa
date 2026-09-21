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
 * {@code X} ink, {@code W} white, {@code w} paper, {@code o} wood, {@code l} light wood,
 * {@code d} dark wood, {@code p} pink, {@code g} leaf; anything else is left clear.
 */
public record PixelArt(String... rows) {
    /** A labor slip on its string — what a pet is working at. */
    public static final PixelArt SLIP = new PixelArt(
        "....XX....",
        "...X..X...",
        "....XX....",
        "....X.....",
        ".XXXXXXXX.",
        "XllllllllX",
        "XlooollllX",
        "XlooollllX",
        "XllllllllX",
        "XldddddllX",
        "XllllllllX",
        "XldddllllX",
        "XllllllllX",
        ".XXXXXXXX.");

    /** A speech bubble with three dots — what a Chiikawa says most, and what an order is. */
    public static final PixelArt SPEECH = new PixelArt(
        "..XXXXXXXXXX..",
        ".XwwwwwwwwwwX.",
        "XwwwwwwwwwwwwX",
        "XwwwwwwwwwwwwX",
        "XwwdwwwdwwwdwX",
        "XwwdwwwdwwwdwX",
        "XwwwwwwwwwwwwX",
        ".XwwwwwwwwwwX.",
        "..XXXXwXXXXX..",
        ".....XwX......",
        "......XX......");

    /** Two sets of paw prints: walking along behind someone. */
    public static final PixelArt PAWS = new PixelArt(
        "X.X.......",
        ".......X.X",
        "XXX.......",
        "XXX....XXX",
        ".X.....XXX",
        "........X.",
        "..X.X.....",
        ".........",
        "..XXX.....",
        "..XXX.....",
        "...X......");

    /** A round cushion: somewhere to sit and stay. */
    public static final PixelArt CUSHION = new PixelArt(
        "..XXXXXXXXXX..",
        ".XppppppppppX.",
        "XppWWppppppppX",
        "XppppppppppppX",
        "XppppppppppppX",
        ".XppppppppppX.",
        "..XXXXXXXXXX..");

    /** A tuft of grass: out in the field, getting on with it. */
    public static final PixelArt TUFT = new PixelArt(
        ".....X......",
        "....XgX...X.",
        "X...XgX..XgX",
        "XgX.XgX..XgX",
        "XgXXgggX.XgX",
        ".XgXgggXXgX.",
        ".XggggggggX.",
        "..XggggggX..",
        ".XXXXXXXXXX.");

    /** A tick: this is the one that is picked. */
    public static final PixelArt CHECK = new PixelArt(
        "........XX",
        ".......XXX",
        "......XXX.",
        "XX...XXX..",
        "XXX.XXX...",
        ".XXXXX....",
        "..XXX.....");

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
            case 'W' -> UiTheme.HIGHLIGHT;
            case 'w' -> UiTheme.PANEL;
            case 'o' -> UiTheme.WOOD;
            case 'l' -> UiTheme.WOOD_LIGHT;
            case 'd' -> UiTheme.WOOD_DARK;
            case 'p' -> UiTheme.ACCENT_SOFT;
            case 'g' -> UiTheme.LEAF;
            default -> 0;
        };
    }
}
