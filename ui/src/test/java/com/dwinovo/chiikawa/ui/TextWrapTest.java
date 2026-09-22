package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class TextWrapTest {
    /** Five pixels a character, as {@link RecordingSurface} has it. */
    private static final ToIntFunction<String> FONT = text -> text.length() * 5;

    @Test
    void chineseBreaksBetweenAnyTwoCharacters() {
        assertEquals(List.of("放一块劳动", "公告板"), TextWrap.wrap("放一块劳动公告板", 25, FONT));
    }

    @Test
    void englishKeepsItsWordsWhole() {
        assertEquals(List.of("Put up a", "labor", "board"), TextWrap.wrap("Put up a labor board", 40, FONT));
    }

    @Test
    void aWordTooLongForAnyLineIsBrokenAnyway() {
        assertEquals(List.of("abcd", "efgh", "ij"), TextWrap.wrap("abcdefghij", 20, FONT));
    }

    @Test
    void textThatFitsIsOneLineAndNothingIsNoLines() {
        assertEquals(List.of("短句"), TextWrap.wrap("短句", 100, FONT));
        assertEquals(List.of(), TextWrap.wrap("", 100, FONT));
    }
}
