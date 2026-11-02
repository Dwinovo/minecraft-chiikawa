package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * How big the album's page is for a window, which the screen lays itself out by and {@code ScreenFitTest} asks of: the picture
 * as big as the window allows, in a frame with its caption under it and the page arrows under that. Nothing here needs the
 * game, so a test can ask.
 */
public final class AlbumLayout {
    /** What size the picture is taken to be: the camera's own, 4 by 3. */
    public static final int PICTURE_WIDTH = 320;
    public static final int PICTURE_HEIGHT = 240;
    /** The lines the caption is given room for. */
    public static final int CAPTION_LINES = 2;
    /** The most a picture is blown up by; past that it is only a blur. */
    private static final double MAX_SCALE = 2.0;
    private static final int MIN_PICTURE_WIDTH = 96;

    private AlbumLayout() {
    }

    /** @return how wide the picture is in a window this big: as big as the room allows, to a floor of a picture still seen */
    public static int pictureWidth(int windowWidth, int windowHeight) {
        int roomWidth = windowWidth - 2 * PanelFit.MARGIN - 2 * UiStyle.PAD;
        // What is left of the height once the title, the caption, the page buttons and the paper round them are paid for.
        int roomHeight = PanelFit.room(windowHeight) - UiStyle.TITLE_H - 3 * UiStyle.PAD - 2 * UiStyle.GAP
            - CAPTION_LINES * UiStyle.LINE - UiStyle.CONTROL_H;
        double scale = Math.min(MAX_SCALE, Math.min((double) roomWidth / PICTURE_WIDTH, (double) roomHeight / PICTURE_HEIGHT));
        return Math.max(MIN_PICTURE_WIDTH, (int) (PICTURE_WIDTH * scale));
    }

    /** @return how tall a picture that wide is */
    public static int pictureHeight(int pictureWidth) {
        return pictureWidth * PICTURE_HEIGHT / PICTURE_WIDTH;
    }

    /** @return how wide the panel is round a picture that wide */
    public static int panelWidth(int pictureWidth) {
        return pictureWidth + 2 * UiStyle.PAD;
    }

    /** @return how tall the panel is round a picture that tall */
    public static int panelHeight(int pictureHeight) {
        return UiStyle.TITLE_H + UiStyle.PAD + pictureHeight + UiStyle.GAP + CAPTION_LINES * UiStyle.LINE
            + UiStyle.GAP + UiStyle.CONTROL_H + UiStyle.PAD;
    }
}
