package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;

/**
 * One small card that floats on its own — what a pet says over its head.
 *
 * <p>An icon for what the work is, a few words for what the pet is at, a bar for how far
 * along it is. The numbers stay off it on purpose: a label read in passing, at whatever
 * distance the pet happens to be, can carry a picture and a length but not arithmetic.
 * Anyone who wants "7 of 11" opens the pet's backpack, where there is room to say it.
 *
 * @param icon what the work is, or {@link Icon#NONE}
 * @param text what the pet is doing
 * @param done work finished, drawn only when {@code total} is positive
 * @param total work that finishes it; not positive means the pet carries no slip
 */
public record Chip(Icon icon, String text, int done, int total) {
    /** How long the bar in a chip is. Fixed, so labels do not each end at a different place. */
    public static final int BAR_W = 24;

    /** A chip that only says what the pet is doing. */
    public static Chip of(String text) {
        return new Chip(Icon.NONE, text, 0, 0);
    }

    /** A chip for a pet working off a slip: what it is, what it is at, how far along. */
    public static Chip working(Icon icon, String text, int done, int total) {
        return new Chip(icon, text, done, total);
    }

    private boolean hasIcon() {
        return icon != Icon.NONE;
    }

    private boolean hasBar() {
        return total > 0;
    }

    public int width(DrawSurface surface) {
        int width = 2 * UiStyle.PAD + surface.textWidth(text);
        if (hasIcon()) {
            width += UiStyle.SLOT + UiStyle.GAP;
        }
        if (hasBar()) {
            width += UiStyle.GAP + BAR_W;
        }
        return width;
    }

    public int height(DrawSurface surface) {
        int content = hasIcon() ? Math.max(UiStyle.SLOT, surface.lineHeight()) : surface.lineHeight();
        return content + 2 * UiStyle.CHIP_PAD;
    }

    /** Draws it centred on {@code centerX}, sitting on {@code bottomY}. */
    public void draw(DrawSurface surface, int centerX, int bottomY) {
        int width = width(surface);
        int height = height(surface);
        int x = centerX - width / 2;
        int y = bottomY - height;
        Ui.panel(surface, x, y, width, height);

        int at = x + UiStyle.PAD;
        if (hasIcon()) {
            Slot.draw(surface, icon, at, UiStyle.centerIn(y, height, UiStyle.SLOT));
            at += UiStyle.SLOT + UiStyle.GAP;
        }
        surface.drawText(text, at, UiStyle.centerIn(y, height, surface.lineHeight()), UiTheme.TEXT);
        at += surface.textWidth(text);
        if (hasBar()) {
            Bar.draw(surface, at + UiStyle.GAP, UiStyle.centerIn(y, height, UiStyle.BAR_H),
                BAR_W, UiStyle.BAR_H, done, total);
        }
    }
}
