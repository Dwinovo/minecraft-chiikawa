package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * The room a chapter's text has on the handbook, which the screen lays the chapter out in and the texts of every language
 * are measured against, see {@code HandbookTextTest}. Nothing here needs the game, so a test can ask.
 */
public final class HandbookLayout {
    public static final int INDEX_W = 142;
    public static final int CONTENT_W = 190;
    /** The tallest the panel gets, on a window with the room. */
    public static final int MAX_HEIGHT = 256;
    /** How tall the scene is when the window has the room, and how short it may get when not. */
    public static final int SCENE_MAX = 84;
    public static final int SCENE_MIN = 38;
    /** The chapter's title and the gap under it. */
    public static final int TITLE_LINE = UiStyle.LINE + UiStyle.GAP;
    /** The line to the website at the bottom. */
    public static final int LINK_H = 12;
    /** The bar beside the text, when there is more of it than the box. */
    public static final int BAR_W = 3;
    /** How wide a line of the chapter's text may be. */
    public static final int TEXT_W = CONTENT_W - BAR_W - UiStyle.GAP;

    private HandbookLayout() {
    }

    /** @return how tall the right-hand side is, from under the panel's title to its bottom padding */
    public static int contentHeight(int panelHeight) {
        return panelHeight - UiStyle.TITLE_H - 2 * UiStyle.PAD;
    }

    /** @return the height the scene and the text share, the chapter's title and the link line taken off */
    public static int sharedRoom(int contentHeight) {
        return contentHeight - TITLE_LINE - 2 * UiStyle.GAP - LINK_H;
    }

    /** @return how many lines of text a chapter may have to be read without the wheel, the scene at its smallest */
    public static int linesWithoutScrolling(int panelHeight) {
        return (sharedRoom(contentHeight(panelHeight)) - SCENE_MIN) / UiStyle.LINE;
    }
}
