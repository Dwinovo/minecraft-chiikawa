package com.dwinovo.chiikawa.entity.brain.errand.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** When a pet last had its owner near, when it last saw them come back from an absence, and the spell of work it last saw. */
class OwnerSeenTest {
    private static final long AWAY = 2400L;
    private static final long LASTING = 200L;
    private static final long SETTLE = 100L;
    private static final long SPELL = 400L;
    private static final long FINISHED = 600L;
    private static final long NEVER = OwnerSeen.NEVER;

    private static OwnerSeen seen(OwnerSeen before, long now, boolean working) {
        return OwnerSeen.seen(before, now, AWAY, working, SETTLE);
    }

    @Test
    void anOwnerSeenForTheFirstTimeHasNotComeBackFromAnywhere() {
        OwnerSeen seen = seen(null, 100L, false);

        assertEquals(new OwnerSeen(100L, NEVER, NEVER, NEVER), seen);
        assertFalse(seen.returned(100L, LASTING));
    }

    @Test
    void anOwnerWhoStaysNearHasNeverComeBack() {
        OwnerSeen seen = seen(null, 0L, false);
        for (long now = 40L; now <= 4000L; now += 40L) {
            seen = seen(seen, now, false);
        }

        assertEquals(4000L, seen.lastSeen());
        assertFalse(seen.returned(4000L, LASTING));
    }

    @Test
    void anOwnerSeenAgainAfterBeingAwayForTheTimeHasComeBackAndThePetIsGladOfItForAWhile() {
        OwnerSeen before = new OwnerSeen(1000L, NEVER, NEVER, NEVER);

        OwnerSeen back = seen(before, 1000L + AWAY, false);

        assertEquals(1000L + AWAY, back.returnedAt());
        assertTrue(back.returned(1000L + AWAY, LASTING));
        assertTrue(back.returned(1000L + AWAY + LASTING, LASTING));
        assertFalse(back.returned(1000L + AWAY + LASTING + 1, LASTING), "the greeting goes stale");
    }

    @Test
    void anAbsenceShorterThanTheTimeIsNotAbsence() {
        OwnerSeen back = seen(new OwnerSeen(1000L, NEVER, NEVER, NEVER), 1000L + AWAY - 1, false);

        assertFalse(back.returned(1000L + AWAY - 1, LASTING));
    }

    @Test
    void aReturnIsRememberedWhileTheOwnerStaysNear() {
        OwnerSeen back = seen(new OwnerSeen(0L, NEVER, NEVER, NEVER), AWAY, false);

        OwnerSeen later = seen(back, AWAY + 40L, false);

        assertEquals(AWAY, later.returnedAt(), "a later look does not take the return back");
        assertEquals(AWAY + 40L, later.lastSeen());
    }

    /** Works for 500 ticks, seen every 40, and then leaves off. */
    private static OwnerSeen workedForAWhile() {
        OwnerSeen seen = seen(null, 0L, true);
        for (long now = 40L; now <= 480L; now += 40L) {
            seen = seen(seen, now, true);
        }
        return seen;
    }

    @Test
    void aLongSpellOfWorkIsFinishedOnceTheOwnerHasLeftOffForAWhile() {
        OwnerSeen seen = workedForAWhile();

        assertEquals(0L, seen.workedFrom());
        assertEquals(480L, seen.workedAt());
        assertFalse(seen.finishedWork(480L + SETTLE - 1, SETTLE, SPELL, FINISHED), "it is a pause in the work, not the end of it");
        assertTrue(seen.finishedWork(480L + SETTLE, SETTLE, SPELL, FINISHED));
        assertTrue(seen.finishedWork(480L + SETTLE + FINISHED, SETTLE, SPELL, FINISHED));
        assertFalse(seen.finishedWork(480L + SETTLE + FINISHED + 1, SETTLE, SPELL, FINISHED), "the news goes stale");
    }

    @Test
    void aShortSpellOfWorkIsNothingToMakeAnythingOf() {
        OwnerSeen seen = seen(seen(null, 0L, true), 40L, true);

        assertFalse(seen.finishedWork(40L + SETTLE, SETTLE, SPELL, FINISHED), "one block placed is not a job of work");
        assertFalse(seen(null, 0L, false).finishedWork(500L, SETTLE, SPELL, FINISHED), "an owner who has not worked has not finished");
    }

    @Test
    void aPauseShorterThanTheSettleTimeIsTheSameSpellOfWork() {
        OwnerSeen seen = seen(seen(null, 0L, true), 40L, true);

        seen = seen(seen, 40L + SETTLE, true);

        assertEquals(0L, seen.workedFrom(), "they took it up again at once");
        assertEquals(40L + SETTLE, seen.workedAt());
    }

    @Test
    void workTakenUpAgainAfterALongPauseIsASpellOfItsOwn() {
        OwnerSeen seen = seen(seen(null, 0L, true), 40L, true);

        seen = seen(seen, 40L + SETTLE + 1, true);

        assertEquals(40L + SETTLE + 1, seen.workedFrom());
    }

    @Test
    void lookingWhileTheOwnerDoesNotWorkChangesNothingOfTheirWork() {
        OwnerSeen worked = workedForAWhile();

        OwnerSeen later = seen(worked, 520L, false);

        assertEquals(worked.workedFrom(), later.workedFrom());
        assertEquals(worked.workedAt(), later.workedAt());
        assertEquals(520L, later.lastSeen());
    }
}
