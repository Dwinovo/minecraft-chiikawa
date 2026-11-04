package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Tabs standing on a panel's top edge at its right end, each one a picture and its name.
 *
 * <p>The picked tab is one piece with what it opens: in the colour of the band it stands on, taller, and with no line
 * between it and the band. The others sit lower in paper of their own, whole above the edge so their pictures are never cut.
 * A strip is drawn in two passes for that reason: the others {@linkplain #drawBehind behind} the panel, the picked one
 * {@linkplain #drawFront over} its edge. Each tab is as wide as its name, so the strip is as wide as the words in the
 * language are.
 */
public final class TabStrip {
    /** How far a tab stands above the panel's edge. */
    public static final int HEIGHT = 22;
    /** The air between two tabs. */
    public static final int GAP = 3;
    /** How far in from the panel's right edge the last tab ends. */
    public static final int INSET = UiStyle.PAD;
    /** How much lower a tab that is not picked sits. */
    public static final int DROP = 4;
    /** How far down a tab that is not picked its picture starts: all sixteen rows above the edge. */
    private static final int FACE_TOP = 2;
    /** And on the picked tab, which stands a little taller. */
    private static final int PICKED_FACE_TOP = 3;
    /** How far the picked tab reaches into the panel, so the two can be joined. */
    private static final int JOIN = 4;
    /** From a tab's edge to its picture: the sticker's line and rim, and air. */
    private static final int IN = 2 + UiStyle.GAP;
    /** Between a tab's picture and its name. */
    private static final int FACE_GAP = 3;
    private static final int RADIUS = Ui.CARD_RADIUS;

    /** What goes on a tab's face: a 16px picture with its top-left corner at x, y. */
    @FunctionalInterface
    public interface Face {
        void draw(DrawSurface surface, int x, int y);
    }

    /**
     * @param face the picture
     * @param label the name, drawn beside it
     */
    public record Tab(Face face, String label) {
    }

    private final Rect panel;
    private final List<Tab> tabs;
    private final ToIntFunction<String> textWidth;

    /**
     * @param panel the panel the tabs stand on
     * @param textWidth how wide the game draws a name, which the strip is laid out by
     */
    public TabStrip(Rect panel, List<Tab> tabs, ToIntFunction<String> textWidth) {
        this.panel = panel;
        this.tabs = List.copyOf(tabs);
        this.textWidth = textWidth;
    }

    /** How wide a tab is for this name. */
    public static int width(ToIntFunction<String> textWidth, String label) {
        return 2 * IN + UiStyle.ICON + FACE_GAP + textWidth.applyAsInt(label);
    }

    /** How wide the whole strip is for these names, the gaps between included. */
    public static int stripWidth(ToIntFunction<String> textWidth, List<String> labels) {
        int total = (labels.size() - 1) * GAP;
        for (String label : labels) {
            total += width(textWidth, label);
        }
        return total;
    }

    /** Where tab {@code index} is, as drawn. */
    public Rect bounds(int index, boolean picked) {
        int x = panel.right() - INSET - stripWidth(textWidth, labels());
        for (int before = 0; before < index; before++) {
            x += width(textWidth, tabs.get(before).label()) + GAP;
        }
        int width = width(textWidth, tabs.get(index).label());
        return picked
            ? new Rect(x, panel.y() - HEIGHT, width, HEIGHT + JOIN)
            : new Rect(x, panel.y() - HEIGHT + DROP, width, HEIGHT);
    }

    /**
     * The tab under the cursor, or -1. The whole column above the edge counts, whether the tab there is picked or not, so a
     * click just above a lower tab is not lost.
     */
    public int at(int mouseX, int mouseY) {
        if (mouseY < panel.y() - HEIGHT || mouseY >= panel.y()) {
            return -1;
        }
        for (int index = 0; index < tabs.size(); index++) {
            Rect tab = bounds(index, false);
            if (mouseX >= tab.x() && mouseX < tab.right()) {
                return index;
            }
        }
        return -1;
    }

    /** The tabs that are not picked, in paper. Draw before the panel. */
    public void drawBehind(DrawSurface surface, int picked) {
        for (int index = 0; index < tabs.size(); index++) {
            if (index != picked) {
                Rect tab = bounds(index, false);
                Ui.sticker(surface, tab.x(), tab.y(), tab.width(), tab.height(), RADIUS, UiTheme.SURFACE);
                face(surface, tabs.get(index), tab, FACE_TOP, UiTheme.TEXT_MUTED);
            }
        }
    }

    /**
     * The picked tab, joined to the panel's top edge. Draw after the panel and what is drawn across its top.
     *
     * @param fill the colour of what the tab opens into, which it is made of
     */
    public void drawFront(DrawSurface surface, int picked, int fill) {
        Rect tab = bounds(picked, true);
        Ui.sticker(surface, tab.x(), tab.y(), tab.width(), tab.height(), RADIUS, fill);
        // Where the tab meets the panel there is no line: its colour runs straight on into what it opens, and the tab's
        // white rim runs on into the panel's.
        surface.fillRect(tab.x() + 1, panel.y() + 1, tab.width() - 2, JOIN - 1, fill);
        surface.fillRect(tab.x() + 1, panel.y(), 1, JOIN, UiTheme.HIGHLIGHT);
        surface.fillRect(tab.right() - 2, panel.y(), 1, JOIN, UiTheme.HIGHLIGHT);
        face(surface, tabs.get(picked), tab, PICKED_FACE_TOP, UiTheme.TEXT);
    }

    private void face(DrawSurface surface, Tab tab, Rect at, int top, int argb) {
        int faceY = at.y() + top;
        tab.face().draw(surface, at.x() + IN, faceY);
        surface.drawText(tab.label(), at.x() + IN + UiStyle.ICON + FACE_GAP,
            UiStyle.centerIn(faceY, UiStyle.ICON, surface.lineHeight()), argb);
    }

    private List<String> labels() {
        return tabs.stream().map(Tab::label).toList();
    }
}
