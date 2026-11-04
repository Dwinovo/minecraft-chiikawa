package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * An on and off switch: a pill with a knob in it, the knob at the right and the pill pink when it is on. Which way it is
 * set is read from where the knob is and not from a word, so it is read at a glance and in every language; what it
 * switches is for the line beside it to say.
 */
public final class Switch {
    public static final int WIDTH = 24;
    public static final int HEIGHT = 13;
    private static final int KNOB = 9;
    /** The air between the pill's line and the knob. */
    private static final int AIR = 1;

    private Switch() {
    }

    /** @param hovered whether the cursor is on it, which it says by a blush on a switch that is off */
    public static void draw(DrawSurface surface, int x, int y, boolean on, boolean hovered) {
        Ui.roundRect(surface, x, y, WIDTH, HEIGHT, HEIGHT / 2, UiTheme.INK);
        int fill = on ? UiTheme.ACCENT : hovered ? UiTheme.ACCENT_PALE : UiTheme.SURFACE;
        Ui.roundRect(surface, x + 1, y + 1, WIDTH - 2, HEIGHT - 2, HEIGHT / 2 - 1, fill);
        int knobX = on ? x + WIDTH - 1 - AIR - KNOB : x + 1 + AIR;
        int knobY = y + (HEIGHT - KNOB) / 2;
        Ui.roundRect(surface, knobX, knobY, KNOB, KNOB, KNOB / 2, UiTheme.INK);
        Ui.roundRect(surface, knobX + 1, knobY + 1, KNOB - 2, KNOB - 2, KNOB / 2 - 1, UiTheme.HIGHLIGHT);
    }
}
