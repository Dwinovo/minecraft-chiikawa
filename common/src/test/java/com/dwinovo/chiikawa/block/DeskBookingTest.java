package com.dwinovo.chiikawa.block;

import net.minecraft.SharedConstants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.qualification.QualificationExam;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

/** A desk's note of its pet, by the clock: held while the exam is on, the results up the morning after, then gone. */
class DeskBookingTest {
    private static final long DAY = 3L;
    private static final DeskBooking SIGNED_UP = new DeskBooking(UUID.randomUUID(), "Chiikawa",
        Identifier.fromNamespaceAndPath("chiikawa", "weeding"), 5, DAY, Optional.empty());

    @Test
    void theDeskIsHeldWhileItsPetCanStillSit() {
        assertTrue(SIGNED_UP.holdsDesk(at(DAY, 2000L)));
        assertEquals(DeskSheet.ANSWER, SIGNED_UP.sheet(at(DAY, 2000L)));
        assertFalse(SIGNED_UP.holdsDesk(at(DAY, QualificationExam.EXAM_UNTIL)), "the exam has closed");
        assertFalse(SIGNED_UP.current(at(DAY + 1, 2000L)), "a pet that never came does not hold the desk for good");
    }

    @Test
    void handingInFreesTheDeskAndTheResultsGoUpTheMorningAfter() {
        DeskBooking handedIn = SIGNED_UP.handedIn(false);

        assertFalse(handedIn.holdsDesk(at(DAY, 3000L)), "the desk is free once the paper is in");
        assertEquals(DeskSheet.NONE, handedIn.sheet(at(DAY, 3000L)), "the paper went with the pet");
        assertEquals(DeskSheet.FAILED, handedIn.sheet(at(DAY + 1, 2000L)));
        assertEquals(DeskSheet.PASSED, SIGNED_UP.handedIn(true).sheet(at(DAY + 1, 2000L)));
        assertFalse(handedIn.current(at(DAY + 1, QualificationExam.RESULTS_UNTIL)), "the results come down at noon");
    }

    private static long at(long day, long timeOfDay) {
        return day * SharedConstants.TICKS_PER_GAME_DAY + timeOfDay;
    }
}
