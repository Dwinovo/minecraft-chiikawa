package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * How tall the labor board is, which the screen lays itself out by and {@code ScreenFitTest} asks of: a row for each slip up,
 * and the board's level and its upgrade under them. Nothing here needs the game, so a test can ask.
 */
public final class BoardLayout {
    private BoardLayout() {
    }

    /** @return how tall the panel is with this many slips up; a board with none keeps the room of one, for the line that says so */
    public static int panelHeight(int slips) {
        int rows = Math.max(1, slips);
        return UiStyle.TITLE_H + UiStyle.PAD
            + rows * UiStyle.ROW_H + (rows - 1) * UiStyle.GAP
            + UiStyle.GAP_SECTION + UiStyle.CONTROL_H + UiStyle.PAD;
    }
}
