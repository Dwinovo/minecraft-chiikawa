package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * How tall the music box is, which the screen lays itself out by and {@code ScreenFitTest} asks of: its list shows as many
 * rows as the window has the room for, up to the most it ever shows, and the rest of the tracks are on further pages.
 * Nothing here needs the game, so a test can ask.
 */
public final class MusicBoxLayout {
    /** How many rows the list shows when the window has the room. */
    public static final int MOST_ROWS = 8;
    /** Rows sit against one another; the tint under the cursor is what separates them. */
    public static final int ROW_H = UiStyle.ROW_H;

    private MusicBoxLayout() {
    }

    /**
     * @param rows how many rows the list shows
     * @param formatHint whether the line on what the box can play is up, which it is when a track could not be read
     * @return how tall the panel is: the list, the line of hint, the mode button and the footer under it
     */
    public static int panelHeight(int rows, boolean formatHint) {
        int hint = formatHint ? UiStyle.LINE + UiStyle.GAP : 0;
        return UiStyle.TITLE_H + UiStyle.PAD + rows * ROW_H + UiStyle.PAD
            + hint + UiStyle.CONTROL_H + UiStyle.GAP + UiStyle.CONTROL_H + UiStyle.PAD;
    }

    /** @return how many rows the list shows in a window this high */
    public static int rows(int windowHeight, boolean formatHint) {
        return PanelFit.rows(windowHeight, panelHeight(0, formatHint), ROW_H, MOST_ROWS);
    }
}
