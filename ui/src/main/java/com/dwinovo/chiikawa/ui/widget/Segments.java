package com.dwinovo.chiikawa.ui.widget;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * One choice out of a few, as a row of words in one sticker with the one in force filled pink. The options sit side by
 * side so they are seen together, and a cell is as wide as its own word needs and no wider than the room leaves: the
 * words of one language are never the width of another's, and a control of equal cells would be as wide as its longest.
 */
public final class Segments {
    public static final int HEIGHT = 15;
    /** The least air on each side of a word in its cell. */
    public static final int PAD = UiStyle.TIGHT;
    private static final int RADIUS = Ui.CARD_RADIUS;

    private Segments() {
    }

    /**
     * How wide the control has to be for these words: each with its air, the lines between them, and the sticker's own.
     * A language whose words need more than the room has to shorten them.
     */
    public static int minWidth(ToIntFunction<String> textWidth, List<String> labels) {
        int width = 2 + labels.size() - 1;
        for (String label : labels) {
            width += textWidth.applyAsInt(label) + 2 * PAD;
        }
        return width;
    }

    /** @return each cell's box: the words and their air, and the rest of the room shared out evenly as more air */
    public static List<Rect> cells(ToIntFunction<String> textWidth, List<String> labels, Rect area) {
        int count = labels.size();
        int slack = area.width() - minWidth(textWidth, labels);
        List<Rect> cells = new ArrayList<>(count);
        int x = area.x() + 1;
        for (int index = 0; index < count; index++) {
            int extra = slack / count + (index < slack % count ? 1 : 0);
            int width = textWidth.applyAsInt(labels.get(index)) + 2 * PAD + extra;
            cells.add(new Rect(x, area.y() + 1, width, area.height() - 2));
            x += width + 1;
        }
        return cells;
    }

    /**
     * @param area where the control goes; the cells share its width
     * @param picked the cell in force, which is pink
     * @param hovered the cell under the cursor, or -1, which blushes unless it is the one in force
     */
    public static void draw(DrawSurface surface, Rect area, List<String> labels, int picked, int hovered) {
        Ui.roundRect(surface, area.x(), area.y(), area.width(), area.height(), RADIUS, UiTheme.INK);
        Ui.roundRect(surface, area.x() + 1, area.y() + 1, area.width() - 2, area.height() - 2, RADIUS - 1, UiTheme.PANEL);
        List<Rect> cells = cells(surface::textWidth, labels, area);
        for (int index = 0; index < cells.size(); index++) {
            Rect cell = cells.get(index);
            if (index == picked || index == hovered) {
                fill(surface, cell, index > 0, index < cells.size() - 1,
                    index == picked ? UiTheme.ACCENT : UiTheme.ACCENT_PALE);
            }
            if (index < cells.size() - 1) {
                surface.fillRect(cell.right(), area.y() + 1, 1, area.height() - 2, UiTheme.INK);
            }
            Ui.textCentered(surface, labels.get(index), cell, index == picked ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
        }
    }

    /** A cell's colour: round where it is at an end of the control and square where it meets another cell. */
    private static void fill(DrawSurface surface, Rect cell, boolean squareLeft, boolean squareRight, int argb) {
        int corner = RADIUS - 1;
        Ui.roundRect(surface, cell.x(), cell.y(), cell.width(), cell.height(), corner, argb);
        if (squareLeft) {
            surface.fillRect(cell.x(), cell.y(), corner, cell.height(), argb);
        }
        if (squareRight) {
            surface.fillRect(cell.right() - corner, cell.y(), corner, cell.height(), argb);
        }
    }
}
