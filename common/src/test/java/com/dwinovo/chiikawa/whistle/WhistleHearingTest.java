package com.dwinovo.chiikawa.whistle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.whistle.WhistleHearing.Hearing;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** What a friend's whistle does to a pet that is not theirs: what its owner's would, once the pet counts them as an ally. */
class WhistleHearingTest {
    private static final UUID ME = UUID.randomUUID();
    private static final PetOwnership THEIRS = new PetOwnership.Owned(UUID.randomUUID());
    private static final double RANGE = 32.0;

    private static Hearing hear(PetOwnership ownership, boolean allied, boolean sameLevel, double distance, boolean seatedAtExam) {
        return WhistleHearing.of(ME, PetDirective.FOLLOW, ownership, allied, sameLevel, distance * distance, RANGE, seatedAtExam);
    }

    @Test
    void aFriendsWhistleIsObeyedByAnAlliedPet() {
        for (PetDirective order : PetDirective.values()) {
            assertEquals(Hearing.OWNED, WhistleHearing.of(ME, order, THEIRS, true, true, 100.0, RANGE, false), order.name());
        }
    }

    @Test
    void aStrangersWhistleIsNotObeyedByAPetThatIsNotAlliedToThem() {
        for (PetDirective order : PetDirective.values()) {
            assertEquals(Hearing.DEAF, WhistleHearing.of(ME, order, THEIRS, false, true, 100.0, RANGE, false), order.name());
        }
    }

    @Test
    void anAlliedPetOutOfEarshotInAnotherWorldOrAtItsExamStillDoesNotHear() {
        assertEquals(Hearing.DEAF, hear(THEIRS, true, true, RANGE + 1, false));
        assertEquals(Hearing.DEAF, hear(THEIRS, true, false, 1.0, false));
        assertEquals(Hearing.DEAF, hear(THEIRS, true, true, 1.0, true));
    }

    @Test
    void beingAlliedMakesNoDifferenceToAWildPet() {
        assertEquals(Hearing.WILD, hear(PetOwnership.WILD, true, true, 10.0, false));
        assertEquals(Hearing.WILD, hear(PetOwnership.WILD, false, true, 10.0, false));
    }
}
