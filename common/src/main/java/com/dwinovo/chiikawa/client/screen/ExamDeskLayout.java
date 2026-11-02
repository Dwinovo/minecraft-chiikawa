package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * How tall the exam desk is, which the screen lays itself out by and {@code ScreenFitTest} asks of: it lists as many pets as
 * the window has the room for, up to the most it ever lists, the likeliest first. Nothing here needs the game, so a test can
 * ask.
 */
public final class ExamDeskLayout {
    /** As many pets as there is room for in a window with the room; an owner with more nearby sees the likeliest first. */
    public static final int MOST_ROWS = 8;
    /** The desk's news, one line. */
    public static final int NEWS_H = 11;

    private ExamDeskLayout() {
    }

    /**
     * @param rows how many pets are listed; at least one's room is kept, for the line that says nobody is there
     * @param tabs whether the desk examines more than one licence, so that a tab each is above the pets
     * @return how tall the panel is: the news, the tabs, the licence and its fee, and the pets under them
     */
    public static int panelHeight(int rows, boolean tabs) {
        return aboveThePets(tabs) + Math.max(1, rows) * UiStyle.ROW_PITCH + UiStyle.PAD;
    }

    /** @return how many of the {@code candidates} are listed in a window this high */
    public static int rows(int windowHeight, boolean tabs, int candidates) {
        return Math.min(candidates,
            PanelFit.rows(windowHeight, aboveThePets(tabs) + UiStyle.PAD, UiStyle.ROW_PITCH, MOST_ROWS));
    }

    /** The title, the news, the tabs and the licence's line, down to where the first pet is. */
    private static int aboveThePets(boolean tabs) {
        int tabsH = tabs ? UiStyle.CONTROL_H + UiStyle.GAP : 0;
        return UiStyle.TITLE_H + UiStyle.PAD + NEWS_H + UiStyle.GAP + tabsH + UiStyle.ROW_H + UiStyle.GAP;
    }
}
