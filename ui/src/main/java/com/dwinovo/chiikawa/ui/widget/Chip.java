package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A small sticker with a picture and a few words in it: a job, a licence, how much money a pet has. The words say what, the
 * picture says which kind of thing it is, and more waits under the cursor.
 *
 * <p>The picture is drawn small, {@link #FACE} wide, so a row of them is not much wider than what is written in them: a
 * row of whole items is a row of wide gaps. A chip can be marked with a pink dot on its corner, for the one thing in a row
 * that wants a look.
 */
public final class Chip {
    /** How tall a chip is: the picture, and the air round it. */
    public static final int HEIGHT = 18;
    /** The box a chip's picture is drawn in. */
    public static final int FACE = 12;
    /** How far in from the chip's edge the picture starts: the sticker's line and rim, and a little air. */
    private static final int IN = 2 + UiStyle.TIGHT;
    /** The pink dot: its width, and how far it sticks out of the corner. */
    private static final int DOT = 5;
    private static final int DOT_OUT = 2;

    /** What goes on a chip's face: a picture with its top-left corner at x, y, no wider than {@link #FACE}. */
    @FunctionalInterface
    public interface Face {
        void draw(DrawSurface surface, int x, int y);
    }

    private Chip() {
    }

    /** What {@link #draw} will take up, for a caller laying out from the right edge. */
    public static int width(DrawSurface surface, String label) {
        return overhead() + surface.textWidth(label);
    }

    /** What a chip takes up beside its words: for a caller that works out how much room they have. */
    public static int overhead() {
        return 2 * IN + FACE + UiStyle.TIGHT;
    }

    /**
     * @param marked whether to put the pink dot on the chip's corner
     * @return the width it took, so a caller can place the next one
     */
    public static int draw(DrawSurface surface, Face face, String label, int x, int y, int fill, boolean marked) {
        int width = width(surface, label);
        Ui.sticker(surface, x, y, width, HEIGHT, Ui.CARD_RADIUS, fill);
        face.draw(surface, x + IN, UiStyle.centerIn(y, HEIGHT, FACE));
        surface.drawText(label, x + IN + FACE + UiStyle.TIGHT, UiStyle.centerIn(y, HEIGHT, surface.lineHeight()), UiTheme.TEXT);
        if (marked) {
            Ui.roundRect(surface, x + width - DOT + DOT_OUT, y - DOT_OUT, DOT, DOT, DOT / 2, UiTheme.INK);
            Ui.roundRect(surface, x + width - DOT + DOT_OUT + 1, y - DOT_OUT + 1, DOT - 2, DOT - 2, DOT / 2 - 1, UiTheme.ACCENT);
        }
        return width;
    }
}
