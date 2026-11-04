package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.widget.Chip;
import java.util.function.ToIntFunction;

/**
 * Where the parts of the pet screen's speech bubble go, for the words in it. A bubble says one thing in one line: what the
 * pet is short of, with a picture of it and, when there is room, its name; or what it is at, with the slip's picture, how far
 * along the slip is and a count; or only what it is at. The bubble is a different width when the pet carries a present,
 * which has a badge of its own beside it, and the words are not the same width in every language, so the row is worked out
 * from them: the words are cut, down to a few letters, before the bar and the count are left out, since how far along a slip
 * is says more than how it is worded. Nothing here needs the game, so a test can ask.
 *
 * <p>Every x is from the bubble's left edge, and each part is only there when its width is more than nothing.
 *
 * @param textWidth how wide the first words are drawn, the most they may take; wider words are cut
 * @param iconX where the picture starts, -1 when there is none
 * @param nameX where the picture's name starts, -1 when there is no room for it
 * @param nameWidth how wide the name may be
 * @param barX where the bar starts
 * @param barWidth how wide it is, nothing when it is left out
 * @param countX where the count starts
 * @param countWidth how wide it is, nothing when it is left out
 */
public record SpeechRow(int textWidth, int iconX, int nameX, int nameWidth, int barX, int barWidth, int countX, int countWidth) {
    /** The air at each end of the bubble and between its parts. */
    static final int AIR = UiStyle.GAP;
    /** How big the picture is. */
    static final int ICON = Chip.FACE;
    /** The narrowest bar worth drawing. */
    static final int LEAST_BAR = 16;
    /** The fewest pixels of words kept to keep the bar and the count: fewer and they are left out instead. */
    static final int LEAST_WORDS = 24;

    private static final SpeechRow NOTHING = new SpeechRow(0, -1, -1, 0, 0, 0, 0, 0);

    /** @return the row for a pet that is short of something: the reason, the picture, and the picture's name if it fits */
    public static SpeechRow lacking(ToIntFunction<String> textWidth, int width, String reason, String name) {
        int inner = width - 2 * AIR;
        int words = Math.min(textWidth.applyAsInt(reason), inner - AIR - ICON);
        int iconX = AIR + words + AIR;
        int nameX = iconX + ICON + AIR;
        int nameWidth = textWidth.applyAsInt(name);
        boolean fits = !name.isEmpty() && nameX + nameWidth <= width - AIR;
        return new SpeechRow(words, iconX, fits ? nameX : -1, fits ? nameWidth : 0, 0, 0, 0, 0);
    }

    /** @return the row for a pet at work on a slip: what it is at, the slip's picture, the bar and the count */
    public static SpeechRow working(ToIntFunction<String> textWidth, int width, String doing, String count) {
        int inner = width - 2 * AIR;
        int countWidth = textWidth.applyAsInt(count);
        int beside = AIR + ICON + AIR + LEAST_BAR + AIR + countWidth;
        int wordsRoom = inner - beside;
        int natural = textWidth.applyAsInt(doing);
        if (wordsRoom < LEAST_WORDS) {
            // No room for a bar that says anything: the words and the picture, as much of the words as there is room for.
            int words = Math.min(natural, inner - AIR - ICON);
            return new SpeechRow(words, AIR + words + AIR, -1, 0, 0, 0, 0, 0);
        }
        int words = Math.min(natural, wordsRoom);
        int iconX = AIR + words + AIR;
        int barX = iconX + ICON + AIR;
        int countX = width - AIR - countWidth;
        return new SpeechRow(words, iconX, -1, 0, barX, countX - AIR - barX, countX, countWidth);
    }

    /** @return the row for a pet that is only at something: the words, as much as there is room for */
    public static SpeechRow saying(ToIntFunction<String> textWidth, int width, String doing) {
        return new SpeechRow(Math.min(textWidth.applyAsInt(doing), width - 2 * AIR), -1, -1, 0, 0, 0, 0, 0);
    }

    /** @return a row with nothing in it */
    public static SpeechRow nothing() {
        return NOTHING;
    }

    /** Whether the row has a picture. */
    public boolean hasIcon() {
        return iconX >= 0;
    }

    /** Whether the row has the bar and the count. */
    public boolean hasBar() {
        return barWidth > 0;
    }
}
