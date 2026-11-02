package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;

/**
 * How tall the settings screen is, which the screen lays itself out by and {@code ScreenFitTest} asks of: three sliders, the
 * note on why they cannot be moved, a switch, the note on the volume and the button to close, the two notes as many lines as
 * their words take in the language. Nothing here needs the game, so a test can ask.
 */
public final class SettingsLayout {
    private SettingsLayout() {
    }

    /**
     * @param hintLines how many lines the note under the sliders takes, of the two it may say
     * @param volumeLines how many lines the note on the volume takes
     * @return how tall the panel is
     */
    public static int panelHeight(int hintLines, int volumeLines) {
        int contentHeight = 3 * UiStyle.CONTROL_H + 2 * UiStyle.GAP
            + UiStyle.GAP + hintLines * UiStyle.LINE
            + UiStyle.GAP_SECTION + UiStyle.CONTROL_H
            + UiStyle.GAP + volumeLines * UiStyle.LINE
            + UiStyle.GAP_SECTION + UiStyle.CONTROL_H;
        return UiStyle.TITLE_H + UiStyle.PAD + contentHeight + UiStyle.PAD;
    }
}
