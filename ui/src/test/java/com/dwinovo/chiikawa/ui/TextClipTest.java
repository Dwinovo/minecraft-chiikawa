package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class TextClipTest {
    /** A font where every character is six pixels wide, ellipsis included. */
    private static final ToIntFunction<String> SIX_PIXELS = text -> text.length() * 6;

    @Test
    void textThatFitsIsLeftAlone() {
        assertEquals("Usagi", TextClip.clip("Usagi", 30, SIX_PIXELS));
        assertEquals("Usagi", TextClip.clip("Usagi", 999, SIX_PIXELS));
    }

    @Test
    void textThatDoesNotFitEndsInAnEllipsis() {
        assertEquals("Usa…", TextClip.clip("Usagi", 24, SIX_PIXELS));
        assertEquals("U…", TextClip.clip("Usagi", 15, SIX_PIXELS));
    }

    @Test
    void roomForNothingDrawsNothing() {
        assertEquals("…", TextClip.clip("Usagi", 6, SIX_PIXELS));
        assertEquals("", TextClip.clip("Usagi", 5, SIX_PIXELS));
        assertEquals("", TextClip.clip("Usagi", 0, SIX_PIXELS));
    }

    @Test
    void theClippedTextNeverOutgrowsItsRoom() {
        for (int maxWidth = 0; maxWidth < 40; maxWidth++) {
            String clipped = TextClip.clip("Hachiware", maxWidth, SIX_PIXELS);
            assertEquals(true, SIX_PIXELS.applyAsInt(clipped) <= maxWidth, () -> "spilled at " + clipped);
        }
    }
}
