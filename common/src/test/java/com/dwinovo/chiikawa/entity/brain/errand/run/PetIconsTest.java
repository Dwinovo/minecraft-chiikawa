package com.dwinovo.chiikawa.entity.brain.errand.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import org.junit.jupiter.api.Test;

/** The picture over a pet's head, as a table of what it is and what it found. */
class PetIconsTest {
    private static final ResourceLocation HARVEST = ResourceLocation.fromNamespaceAndPath("chiikawa", "harvest_block");
    private static final WhyNot SEEKING = new WhyNot(HARVEST, "intent.chiikawa.fail.no_crop", Optional.empty(), 0L);
    private static final WhyNot MISSING = new WhyNot(HARVEST, "intent.chiikawa.fail.no_arrows",
        Optional.of(LackKey.tag(ItemTags.ARROWS)), 0L);

    @Test
    void aPetWithNothingOnItsMindShowsNothing() {
        assertEquals(PetIcon.NONE, PetIcons.of(Set.of(), PetIcons.Signs.NONE));
        assertEquals(PetIcon.NONE, PetIcons.of(Set.of(StepTag.SEATED, StepTag.PLAYING_MUSIC, StepTag.IN_SCENE), PetIcons.Signs.NONE));
    }

    @Test
    void aFrightenedPetShowsItWhateverElseIsOnItsMind() {
        PetIcons.Signs full = new PetIcons.Signs(true, true, Optional.of(MISSING), DayPhase.DAY);

        assertEquals(PetIcon.SCARED, PetIcons.of(Set.of(StepTag.SCARED), full));
    }

    @Test
    void aPresentInThePawsOrInMindAndReadyShowsAGiftBeforeAnythingAboutWork() {
        assertEquals(PetIcon.GIFT, PetIcons.of(Set.of(), new PetIcons.Signs(true, false, Optional.of(MISSING), DayPhase.DAY)));
        assertEquals(PetIcon.GIFT, PetIcons.of(Set.of(), new PetIcons.Signs(false, true, Optional.of(SEEKING), DayPhase.DAY)));
    }

    @Test
    void aPetShortOfSomethingShowsItBeforeShowingThatItIsLookingForWork() {
        assertEquals(PetIcon.MISSING, PetIcons.of(Set.of(), new PetIcons.Signs(false, false, Optional.of(MISSING), DayPhase.DAY)));
    }

    @Test
    void aPetWithNoWorkToDoByDayIsLookingForSome() {
        for (DayPhase phase : new DayPhase[] {DayPhase.MORNING, DayPhase.DAY, DayPhase.EVENING}) {
            assertEquals(PetIcon.SEEKING_WORK, PetIcons.of(Set.of(), new PetIcons.Signs(false, false, Optional.of(SEEKING), phase)),
                phase::name);
        }
    }

    @Test
    void atNightAPetWithNoWorkIsNotLookingForAnyButOneShortOfSomethingStillShowsIt() {
        assertEquals(PetIcon.NONE, PetIcons.of(Set.of(), new PetIcons.Signs(false, false, Optional.of(SEEKING), DayPhase.NIGHT)));
        assertEquals(PetIcon.MISSING, PetIcons.of(Set.of(), new PetIcons.Signs(false, false, Optional.of(MISSING), DayPhase.NIGHT)));
    }

    @Test
    void everyIconHasAnIdThatComesBackToIt() {
        for (PetIcon icon : PetIcon.values()) {
            assertEquals(icon, PetIcon.byId(icon.id()));
        }
        assertEquals(PetIcon.NONE, PetIcon.byId(99));
        assertEquals(PetIcon.NONE, PetIcon.byId(-1));
    }
}
