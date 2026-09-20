package com.dwinovo.chiikawa.ui;

import java.util.function.ToIntFunction;

/**
 * Cutting a line of text to the room it has. Kept apart from drawing so it can be tested
 * against a made-up font.
 */
public final class TextClip {
    private static final String ELLIPSIS = "…";

    private TextClip() {
    }

    /**
     * @param text the line to fit
     * @param maxWidth room available in pixels
     * @param width how wide a string is in the font it will be drawn with
     * @return {@code text} when it fits, otherwise as much of it as fits with an ellipsis;
     *         the ellipsis alone when even that does not fit, and nothing when nothing does
     */
    public static String clip(String text, int maxWidth, ToIntFunction<String> width) {
        if (maxWidth <= 0) {
            return "";
        }
        if (width.applyAsInt(text) <= maxWidth) {
            return text;
        }
        int ellipsis = width.applyAsInt(ELLIPSIS);
        if (ellipsis > maxWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && width.applyAsInt(text.substring(0, end)) + ellipsis > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ELLIPSIS;
    }
}
