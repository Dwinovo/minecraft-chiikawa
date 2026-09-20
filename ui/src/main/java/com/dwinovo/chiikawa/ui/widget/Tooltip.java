package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.List;

/**
 * The card that answers "and what else?" — where the detail lives that would otherwise be
 * written into every row.
 *
 * <p>A list whose rows each carry their job, their count and who took them is a list
 * nobody scans: every row is the same weight, so the eye has to read all of it. Put the
 * one thing that tells rows apart in the row and the rest under the cursor, and the list
 * can be scanned in a second while losing nothing — the detail is a hand movement away.
 *
 * <p>The first line is the subject and is drawn as such; the rest support it and are
 * drawn quieter.
 */
public final class Tooltip {
    private Tooltip() {
    }

    /** How far from the cursor the card sits, so it never hides what is being pointed at. */
    private static final int CURSOR_GAP = UiStyle.GAP_SECTION;

    public static int width(DrawSurface surface, List<String> lines) {
        int text = 0;
        for (String line : lines) {
            text = Math.max(text, surface.textWidth(line));
        }
        return text + 2 * UiStyle.PAD;
    }

    public static int height(DrawSurface surface, List<String> lines) {
        return 2 * UiStyle.PAD + (lines.size() - 1) * UiStyle.LINE + surface.lineHeight();
    }

    /**
     * Where the card goes for a cursor at {@code mouseX, mouseY}: beside it, and inside the
     * screen even when the cursor is at an edge — a card that runs off the screen takes the
     * answer with it.
     */
    public static Rect place(DrawSurface surface, List<String> lines,
                             int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int width = width(surface, lines);
        int height = height(surface, lines);
        int x = Math.max(0, Math.min(mouseX + CURSOR_GAP, screenWidth - width));
        int y = Math.max(0, Math.min(mouseY - height / 2, screenHeight - height));
        return new Rect(x, y, width, height);
    }

    public static void draw(DrawSurface surface, List<String> lines,
                            int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (lines.isEmpty()) {
            return;
        }
        Rect at = place(surface, lines, mouseX, mouseY, screenWidth, screenHeight);
        Ui.card(surface, at.x(), at.y(), at.width(), at.height());
        int y = at.y() + UiStyle.PAD;
        for (int i = 0; i < lines.size(); i++) {
            surface.drawText(lines.get(i), at.x() + UiStyle.PAD, y, i == 0 ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
            y += UiStyle.LINE;
        }
    }
}
