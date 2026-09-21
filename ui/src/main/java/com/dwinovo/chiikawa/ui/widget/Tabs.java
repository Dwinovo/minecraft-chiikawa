package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.List;

/**
 * Tabs along a panel's top edge, each one a picture with its name waiting under the
 * cursor.
 *
 * <p>The picked tab is one piece with the panel: paper-coloured, taller, and with no line
 * between it and what it opens. The others sit lower in pale colours of their own, whole
 * above the edge so their pictures are never cut. A tab strip is drawn in two passes for
 * that reason — the others {@linkplain #drawBehind behind} the panel, the picked one
 * {@linkplain #drawFront over} its edge.
 */
public final class Tabs {
    public static final int WIDTH = 28;
    public static final int HEIGHT = 22;
    /** The air between two tabs. */
    public static final int GAP = 3;
    /** How far in from the panel's left edge the first tab starts. */
    public static final int INSET = UiStyle.PAD;
    /** How much lower an unpicked tab sits. */
    public static final int DROP = 4;
    /** How far down an unpicked tab its picture starts: all sixteen rows above the edge. */
    public static final int FACE_TOP = 2;
    /** And on the picked tab, which stands a little taller. */
    private static final int PICKED_FACE_TOP = 3;
    /** How far the picked tab reaches into the panel, so the two can be joined. */
    private static final int JOIN = 4;
    private static final int RADIUS = Ui.CARD_RADIUS;

    /** What goes on a tab's face: a 16px picture with its top-left corner at x, y. */
    @FunctionalInterface
    public interface Face {
        void draw(DrawSurface surface, int x, int y);
    }

    private Tabs() {
    }

    /** Where tab {@code index} is, as drawn. */
    public static Rect bounds(int panelX, int panelY, int index, boolean picked) {
        int x = panelX + INSET + index * (WIDTH + GAP);
        return picked
            ? new Rect(x, panelY - HEIGHT, WIDTH, HEIGHT + JOIN)
            : new Rect(x, panelY - HEIGHT + DROP, WIDTH, HEIGHT);
    }

    /**
     * The tab under the cursor, or -1. The whole column above the edge counts, whether the
     * tab there is picked or not, so a click just above a lower tab is not lost.
     */
    public static int at(int panelX, int panelY, int count, int mouseX, int mouseY) {
        if (mouseY < panelY - HEIGHT || mouseY >= panelY) {
            return -1;
        }
        for (int index = 0; index < count; index++) {
            int x = panelX + INSET + index * (WIDTH + GAP);
            if (mouseX >= x && mouseX < x + WIDTH) {
                return index;
            }
        }
        return -1;
    }

    /** The tabs that are not picked, each in its own pale colour. Draw before the panel. */
    public static void drawBehind(DrawSurface surface, int panelX, int panelY, List<Face> faces, int[] tints,
            int picked) {
        for (int index = 0; index < faces.size(); index++) {
            if (index == picked) {
                continue;
            }
            Rect tab = bounds(panelX, panelY, index, false);
            Ui.sticker(surface, tab.x(), tab.y(), tab.width(), tab.height(), RADIUS, tints[index]);
            faces.get(index).draw(surface, tab.x() + (WIDTH - UiStyle.ICON) / 2, tab.y() + FACE_TOP);
        }
    }

    /** The picked tab, joined to the panel's top edge. Draw after the panel. */
    public static void drawFront(DrawSurface surface, int panelX, int panelY, Face face, int picked) {
        Rect tab = bounds(panelX, panelY, picked, true);
        Ui.sticker(surface, tab.x(), tab.y(), tab.width(), tab.height(), RADIUS, UiTheme.PANEL);
        // Where the tab meets the panel there is no line: paper runs straight on, and the
        // tab's white rim runs on into the panel's.
        surface.fillRect(tab.x() + 1, panelY + 1, WIDTH - 2, JOIN - 1, UiTheme.PANEL);
        surface.fillRect(tab.x() + 1, panelY, 1, JOIN, UiTheme.HIGHLIGHT);
        surface.fillRect(tab.x() + WIDTH - 2, panelY, 1, JOIN, UiTheme.HIGHLIGHT);
        face.draw(surface, tab.x() + (WIDTH - UiStyle.ICON) / 2, tab.y() + PICKED_FACE_TOP);
    }
}
