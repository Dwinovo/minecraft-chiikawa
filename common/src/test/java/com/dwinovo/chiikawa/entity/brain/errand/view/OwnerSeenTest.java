package com.dwinovo.chiikawa.entity.brain.errand.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** When a pet last had its owner near, and when it last saw them come back from an absence. */
class OwnerSeenTest {
    private static final long AWAY = 2400L;
    private static final long LASTING = 200L;

    @Test
    void anOwnerSeenForTheFirstTimeHasNotComeBackFromAnywhere() {
        OwnerSeen seen = OwnerSeen.seen(null, 100L, AWAY);

        assertEquals(new OwnerSeen(100L, OwnerSeen.NEVER), seen);
        assertFalse(seen.returned(100L, LASTING));
    }

    @Test
    void anOwnerWhoStaysNearHasNeverComeBack() {
        OwnerSeen seen = OwnerSeen.seen(null, 0L, AWAY);
        for (long now = 40L; now <= 4000L; now += 40L) {
            seen = OwnerSeen.seen(seen, now, AWAY);
        }

        assertEquals(4000L, seen.lastSeen());
        assertFalse(seen.returned(4000L, LASTING));
    }

    @Test
    void anOwnerSeenAgainAfterBeingAwayForTheTimeHasComeBackAndThePetIsGladOfItForAWhile() {
        OwnerSeen before = new OwnerSeen(1000L, OwnerSeen.NEVER);

        OwnerSeen back = OwnerSeen.seen(before, 1000L + AWAY, AWAY);

        assertEquals(1000L + AWAY, back.returnedAt());
        assertTrue(back.returned(1000L + AWAY, LASTING));
        assertTrue(back.returned(1000L + AWAY + LASTING, LASTING));
        assertFalse(back.returned(1000L + AWAY + LASTING + 1, LASTING), "the greeting goes stale");
    }

    @Test
    void anAbsenceShorterThanTheTimeIsNotAbsence() {
        OwnerSeen back = OwnerSeen.seen(new OwnerSeen(1000L, OwnerSeen.NEVER), 1000L + AWAY - 1, AWAY);

        assertFalse(back.returned(1000L + AWAY - 1, LASTING));
    }

    @Test
    void aReturnIsRememberedWhileTheOwnerStaysNear() {
        OwnerSeen back = OwnerSeen.seen(new OwnerSeen(0L, OwnerSeen.NEVER), AWAY, AWAY);

        OwnerSeen later = OwnerSeen.seen(back, AWAY + 40L, AWAY);

        assertEquals(AWAY, later.returnedAt(), "a later look does not take the return back");
        assertEquals(AWAY + 40L, later.lastSeen());
    }
}
