package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.widget.Chip;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Where the first row of the pet screen's banner puts things for the words they have, and what gives when it is crowded. */
class HeaderRowTest {
    /** Five pixels a character, as a stand-in for the game's font; CJK pets are nine, so a name here is five a letter. */
    private static int width(String text) {
        return text.length() * 5;
    }

    private static HeaderRow place(String name, String job, String rank, String money, String eager) {
        return HeaderRow.place(HeaderRowTest::width, name, job, true, Optional.ofNullable(rank), money, Optional.ofNullable(eager));
    }

    @Test
    void theBadgesAreAsWideAsTheirWordsAndEndAtTheRightEdgeEachBesideTheLast() {
        HeaderRow row = place("Usagi", "Farmer", null, "12", "3:20");

        assertEquals(Chip.overhead() + width("3:20"), row.eagerWidth());
        assertEquals(PetScreenLayout.RIGHT, row.eagerX() + row.eagerWidth());
        assertEquals(Chip.overhead() + width("12"), row.moneyWidth());
        assertEquals(row.eagerX() - HeaderRow.CHIP_GAP, row.moneyX() + row.moneyWidth());
    }

    @Test
    void aPetThatIsNotKeenHasNoBadgeForIt() {
        HeaderRow row = place("Usagi", "Farmer", null, "12", null);

        assertEquals(0, row.eagerWidth());
        assertEquals(PetScreenLayout.RIGHT, row.moneyX() + row.moneyWidth());
    }

    @Test
    void aNameThatFitsIsDrawnAtTwiceTheSizeAndTheChipsFollowIt() {
        HeaderRow row = place("Usagi", "Farmer", "Gr 4", "12", null);

        assertEquals(2, row.nameScale());
        assertEquals(2 * width("Usagi"), row.nameWidth());
        assertEquals(PetScreenLayout.TEXT_X + row.nameWidth() + HeaderRow.NAME_GAP, row.jobX());
        assertEquals(Chip.overhead() + width("Farmer"), row.jobWidth());
        assertEquals(row.jobX() + row.jobWidth() + HeaderRow.CHIP_GAP, row.rankX());
        assertEquals(Chip.overhead() + width("Gr 4"), row.rankWidth());
    }

    @Test
    void aNameTooWideToBeBigIsDrawnAtTheSizeOfTheRestWhole() {
        HeaderRow row = place("Hachiwarehachiware", "Farmer", null, "12", null);

        assertEquals(1, row.nameScale());
        assertEquals(width("Hachiwarehachiware"), row.nameWidth());
        assertEquals(Chip.overhead() + width("Farmer"), row.jobWidth(), "the chips lost words they did not have to");
    }

    @Test
    void theRowNeverRunsIntoTheBadgesWhateverTheWords() {
        for (String name : new String[] {"Usagi", "Furuhonya", "A name somebody gave a pet and is very long indeed"}) {
            for (String job : new String[] {"Farmer", "Agriculture and all that goes with it"}) {
                for (String eager : new String[] {null, "12:20"}) {
                    HeaderRow row = place(name, job, "Grade 10", "1234", eager);
                    int end = row.rankX() + row.rankWidth();
                    assertTrue(end <= row.moneyX() - UiStyle.GAP || row.nameWidth() == 0 && end <= row.moneyX(),
                        name + " / " + job + " / " + eager + " ends at " + end + ", the money starts at " + row.moneyX());
                }
            }
        }
    }

    @Test
    void whenCrowdedTheChipsGiveUpWordsFirstButKeepSomeAndThenTheNameIsCut() {
        HeaderRow row = place("A name somebody gave a pet and is very long indeed", "Agriculture", "Grade 10", "1234", "12:20");

        assertTrue(row.jobWidth() < Chip.overhead() + width("Agriculture"), "the job kept all its words");
        assertTrue(HeaderRow.wordsIn(row.jobWidth()) >= HeaderRow.LEAST_WORDS, "the job lost all its words");
        assertTrue(HeaderRow.wordsIn(row.rankWidth()) >= HeaderRow.LEAST_WORDS, "the licence lost all its words");
        assertTrue(row.nameWidth() < width("A name somebody gave a pet and is very long indeed"), "the name was not cut");
    }

    @Test
    void aJobWithNoPictureIsOnlyItsWordsAndTheAirRoundThem() {
        HeaderRow row = HeaderRow.place(HeaderRowTest::width, "Usagi", "None", false, Optional.empty(), "12", Optional.empty());

        assertEquals(Chip.overhead(false) + width("None"), row.jobWidth());
        assertTrue(row.jobWidth() < Chip.overhead() + width("None"));
        assertEquals(width("None"), row.jobWords());
    }

    @Test
    void withNoLicenceThereIsNoChipForItAndTheNameHasTheRoom() {
        HeaderRow with = place("Hachiware", "Farmer", "Gr 4", "12", "3:20");
        HeaderRow without = place("Hachiware", "Farmer", null, "12", "3:20");

        assertEquals(0, without.rankWidth());
        assertFalse(without.nameWidth() < with.nameWidth());
    }
}
