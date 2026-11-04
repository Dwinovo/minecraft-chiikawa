package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Where the parts of the pet screen's speech bubble go for the words in it, and what is left out when they do not fit. */
class SpeechRowTest {
    private static final int WIDE = PetScreenLayout.bubble(false).width();
    private static final int NARROW = PetScreenLayout.bubble(true).width();

    /** Five pixels a character, as a stand-in for the game's font. */
    private static int width(String text) {
        return text.length() * 5;
    }

    @Test
    void whatIsMissingIsSaidThenPicturedThenNamedWhenThereIsRoomForTheName() {
        SpeechRow row = SpeechRow.lacking(SpeechRowTest::width, WIDE, "Needs seeds", "Wheat Seeds");

        assertEquals(width("Needs seeds"), row.textWidth());
        assertEquals(SpeechRow.AIR + row.textWidth() + SpeechRow.AIR, row.iconX());
        assertEquals(row.iconX() + SpeechRow.ICON + SpeechRow.AIR, row.nameX());
        assertEquals(width("Wheat Seeds"), row.nameWidth());
        assertTrue(row.nameX() + row.nameWidth() <= WIDE - SpeechRow.AIR);
    }

    @Test
    void theNameIsLeftOutWhenItWouldRunOutOfTheBubbleAndThePictureStays() {
        SpeechRow row = SpeechRow.lacking(SpeechRowTest::width, NARROW, "Needs plant crops", "Wheat Seeds And More Seeds");

        assertTrue(row.hasIcon());
        assertEquals(-1, row.nameX());
        assertEquals(0, row.nameWidth());
    }

    @Test
    void aSlipHasTheWordsThePictureTheBarAndTheCountToTheRightEdge() {
        SpeechRow row = SpeechRow.working(SpeechRowTest::width, WIDE, "Pulling weeds", "12/32");

        assertEquals(width("Pulling weeds"), row.textWidth());
        assertTrue(row.hasIcon() && row.hasBar());
        assertEquals(row.iconX() + SpeechRow.ICON + SpeechRow.AIR, row.barX());
        assertEquals(row.barX() + row.barWidth() + SpeechRow.AIR, row.countX());
        assertEquals(WIDE - SpeechRow.AIR, row.countX() + row.countWidth(), "the count is not at the right edge");
        assertTrue(row.barWidth() >= SpeechRow.LEAST_BAR);
    }

    @Test
    void theBarTakesWhatTheWordsLeaveAndNeverLessThanItsLeast() {
        SpeechRow room = SpeechRow.working(SpeechRowTest::width, WIDE, "A", "1/2");
        SpeechRow tight = SpeechRow.working(SpeechRowTest::width, WIDE, "x".repeat(40), "180/300");

        assertTrue(room.barWidth() > tight.barWidth());
        assertEquals(SpeechRow.LEAST_BAR, tight.barWidth());
        assertTrue(tight.textWidth() < width("x".repeat(40)), "the words were not cut for the bar");
    }

    @Test
    void withTooLittleRoomForTheWordsOfAnyUseTheBarAndTheCountAreLeftOutAndTheWordsKeepWhatIsLeft() {
        SpeechRow row = SpeechRow.working(SpeechRowTest::width, 90, "Picking up", "180/300");

        assertFalse(row.hasBar());
        assertTrue(row.hasIcon());
        assertEquals(width("Picking up"), row.textWidth());
        assertTrue(row.iconX() + SpeechRow.ICON <= 90 - SpeechRow.AIR);
    }

    @Test
    void inTheBubbleBesideAPresentTheWordsAreCutBeforeTheBarIsLeftOut() {
        SpeechRow row = SpeechRow.working(SpeechRowTest::width, NARROW, "Picking up an item", "180/300");

        assertTrue(row.hasBar());
        assertTrue(row.textWidth() < width("Picking up an item"));
        assertTrue(row.textWidth() >= SpeechRow.LEAST_WORDS);
    }

    @Test
    void wordsAloneAreAsMuchAsThereIsRoomFor() {
        assertEquals(width("Idle"), SpeechRow.saying(SpeechRowTest::width, WIDE, "Idle").textWidth());
        assertEquals(WIDE - 2 * SpeechRow.AIR, SpeechRow.saying(SpeechRowTest::width, WIDE, "x".repeat(100)).textWidth());
        assertFalse(SpeechRow.saying(SpeechRowTest::width, WIDE, "Idle").hasIcon());
    }
}
