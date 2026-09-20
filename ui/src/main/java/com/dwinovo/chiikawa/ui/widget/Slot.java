package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * An icon in a well of its own — the game's own way of saying "a thing", and the reason a
 * row of slips can be told apart before a word of it is read.
 */
public final class Slot {
    private Slot() {
    }

    public static void draw(DrawSurface surface, Icon icon, int x, int y) {
        Ui.well(surface, x, y, UiStyle.SLOT, UiStyle.SLOT);
        surface.drawIcon(icon, x + UiStyle.BORDER, y + UiStyle.BORDER);
    }
}
