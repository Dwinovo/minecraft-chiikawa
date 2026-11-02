package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.widget.TitledPanel;

/**
 * How a screen's panel is fitted to the window it opens in, which the screens that have rows to give up lay themselves out
 * by and the test of {@code ScreenFitTest} asks of every one of them. Nothing here needs the game, so a test can ask.
 */
public final class PanelFit {
    /**
     * How high the smallest window is that the game picks a scale for: its automatic scale is the largest that leaves the
     * window 320 by 240 scaled pixels, so a screen has to fit 240 high, as a 1280 by 720 window at scale 3 is exactly that.
     */
    public static final int SMALLEST_WINDOW = 240;
    /** Kept clear between a panel, and the board hung over its top edge, and the window's edges. */
    public static final int MARGIN = 4;

    private PanelFit() {
    }

    /** @return the tallest a panel can be in a window this high, with the board over its top edge and the margins kept */
    public static int room(int windowHeight) {
        return windowHeight - TitledPanel.SIGN_RISE - 2 * MARGIN;
    }

    /**
     * @param other how tall the panel is with no rows in it
     * @param pitch how much room a row takes
     * @param most how many rows there are to show
     * @return how many of the rows fit in a panel in a window this high: at least one, so that the panel is never empty
     */
    public static int rows(int windowHeight, int other, int pitch, int most) {
        return Math.max(1, Math.min(most, (room(windowHeight) - other) / pitch));
    }

    /** @return the y of a panel's top edge: in the middle of the window, and never so high that its board is cut off */
    public static int top(int windowHeight, int panelHeight) {
        return Math.max(TitledPanel.SIGN_RISE + MARGIN, (windowHeight - panelHeight) / 2);
    }
}
