package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * A value picked by dragging along a groove, with what it stands for written across it:
 * "Chatter: 100%" says both the setting and where it is, which a bare knob never does.
 *
 * <p>A well with the pink poured into it as far as the knob, and the knob a small sticker
 * on top. Pink because the setting is the one thing on its row to touch; it goes pale and
 * the knob flat when there is nothing to change, so a slider that cannot be moved does not
 * look as if it can.
 */
public final class Slider {
    /** How wide the knob is. The knob rides the groove, so the travel is the width less this. */
    public static final int KNOB_W = 8;

    private Slider() {
    }

    /**
     * Where along the groove a cursor is, from 0 at the knob's far left to 1 at its far
     * right: the knob's own middle follows the cursor, so it stays under it while dragged.
     */
    public static double fractionAt(Rect area, int mouseX) {
        double travel = area.width() - KNOB_W;
        if (travel <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (mouseX - area.x() - KNOB_W / 2.0) / travel));
    }

    /** The knob's left edge for a value from 0 to 1. */
    public static int knobX(Rect area, double fraction) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        return area.x() + (int) Math.round(clamped * (area.width() - KNOB_W));
    }

    /**
     * @param fraction how far along, from 0 to 1
     * @param label what it stands for and where it is, centred over the groove
     * @param hovered whether the cursor (or the keyboard) is on it
     * @param active whether it can be moved
     */
    public static void draw(DrawSurface surface, Rect area, double fraction, String label, boolean hovered,
            boolean active) {
        Ui.well(surface, area.x(), area.y(), area.width(), area.height());
        int knobX = knobX(area, fraction);
        if (active) {
            Ui.roundRect(surface, area.x() + 1, area.y() + 1, knobX + KNOB_W / 2 - area.x() - 1, area.height() - 2,
                Ui.WELL_RADIUS, UiTheme.ACCENT_SOFT);
        }
        int knobFill = !active ? UiTheme.SHADE : hovered ? UiTheme.ACCENT : UiTheme.PANEL;
        Ui.sticker(surface, knobX, area.y(), KNOB_W, area.height(), Ui.CARD_RADIUS, knobFill);
        Ui.textCentered(surface, label, area, active ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
    }
}
