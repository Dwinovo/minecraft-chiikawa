package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * A card with a name: the name written on a wooden board hung across the card's top
 * edge, a header row beside it for whatever the screen wants to say at a glance, and a
 * dashed line under that where the contents begin.
 *
 * <p>The name goes on a board rather than on the card because it is a name, not a line
 * of the contents — the same board the pet screen hangs its pet's name on.
 */
public final class TitledPanel {
    /** How far the board hangs above the card's top edge. */
    private static final int SIGN_RISE = 9;

    private TitledPanel() {
    }

    /** @return the y the card's contents start at */
    public static int draw(DrawSurface surface, int x, int y, int width, int height, String title) {
        Ui.card(surface, x, y, width, height);
        Sign.draw(surface, x + UiStyle.PAD, y - SIGN_RISE, title);
        Ui.divider(surface, x, y + UiStyle.TITLE_H, width);
        return y + UiStyle.TITLE_H + UiStyle.PAD;
    }
}
