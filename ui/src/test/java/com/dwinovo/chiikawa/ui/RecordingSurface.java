package com.dwinovo.chiikawa.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the game's drawing API: five pixels a character, nine tall. A test asserts
 * the boxes and the words that came out, which is the whole of what a widget decides —
 * everything else about drawing belongs to Minecraft and is not ours to check.
 */
public final class RecordingSurface implements DrawSurface {
    public record Rectangle(int x, int y, int width, int height, int argb) {
    }

    public record Text(String text, int x, int y, int argb) {
    }

    public record DrawnIcon(Icon icon, int x, int y) {
    }

    public final List<Rectangle> rects = new ArrayList<>();
    public final List<Text> texts = new ArrayList<>();
    public final List<DrawnIcon> icons = new ArrayList<>();

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        rects.add(new Rectangle(x, y, width, height, argb));
    }

    @Override
    public void drawText(String text, int x, int y, int argb) {
        texts.add(new Text(text, x, y, argb));
    }

    @Override
    public void drawIcon(Icon icon, int x, int y) {
        icons.add(new DrawnIcon(icon, x, y));
    }

    @Override
    public int textWidth(String text) {
        return text.length() * 5;
    }

    @Override
    public int lineHeight() {
        return 9;
    }

    /** The one rectangle of that colour, for a test that cares about a single layer. */
    public Rectangle rectOf(int argb) {
        return rects.stream().filter(rect -> rect.argb() == argb).findFirst()
            .orElseThrow(() -> new AssertionError("no rectangle of colour " + Integer.toHexString(argb) + " in " + rects));
    }

    /**
     * The colour a pixel ends up, the last rectangle over it winning, or 0 where nothing
     * was drawn. What a test of a shape wants is what shows, not which calls made it.
     */
    public int colorAt(int x, int y) {
        for (int i = rects.size() - 1; i >= 0; i--) {
            Rectangle rect = rects.get(i);
            if (x >= rect.x() && x < rect.x() + rect.width() && y >= rect.y() && y < rect.y() + rect.height()) {
                return rect.argb();
            }
        }
        return 0;
    }

    public boolean hasRectOf(int argb) {
        return rects.stream().anyMatch(rect -> rect.argb() == argb);
    }
}
