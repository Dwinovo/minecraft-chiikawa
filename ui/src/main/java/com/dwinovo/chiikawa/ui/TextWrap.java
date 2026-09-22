package com.dwinovo.chiikawa.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Breaking a caption into lines that fit. Chinese breaks between any two characters;
 * English is kept to whole words where a line has a space to break at. Kept apart from
 * drawing so it can be tested against a made-up font.
 */
public final class TextWrap {
    private TextWrap() {
    }

    /**
     * @param text what to break up
     * @param maxWidth room a line has, in pixels
     * @param width how wide a string is in the font it will be drawn with
     * @return the lines, none for empty text; a word wider than a whole line is broken
     *         where it has to be rather than left spilling out
     */
    public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> width) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = start;
            while (end < text.length() && width.applyAsInt(text.substring(start, end + 1)) <= maxWidth) {
                end++;
            }
            if (end == start) {
                // Not even one character fits: it goes on a line of its own, or nothing
                // would ever move on.
                end = start + 1;
            } else if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space > start && text.charAt(end) != ' ') {
                    end = space;
                }
            }
            lines.add(text.substring(start, end).strip());
            start = end;
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }
        return lines;
    }
}
