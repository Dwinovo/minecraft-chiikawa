package com.dwinovo.chiikawa.anim.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PetAnimatorParallelTest {

    @Test
    void parallelPhaseSeedIsLatchedOnFirstCall() {
        PetAnimator animator = new PetAnimator();
        animator.ensureParallelPhase(123L);
        long first = animator.getParallelPhaseSeed();

        // Subsequent calls with a different seed must not move the latched value
        // — moving startTimeNs would let two extracts in the same frame produce
        // different animation phases, breaking the double-extract contract.
        animator.ensureParallelPhase(987_654L);
        assertEquals(first, animator.getParallelPhaseSeed());
    }

    @Test
    void differentEntitiesGetDifferentParallelPhases() {
        PetAnimator a = new PetAnimator();
        PetAnimator b = new PetAnimator();
        // Two seeds whose mod-10000 values differ — the spread is 0..10s
        // so this guarantees a delta of at least one millisecond.
        a.ensureParallelPhase(1L);
        b.ensureParallelPhase(5_000L);
        assertNotEquals(a.getParallelPhaseSeed(), b.getParallelPhaseSeed());
    }

    @Test
    void negativeUniquenessSeedStillLandsInTenSecondWindow() {
        PetAnimator animator = new PetAnimator();
        long now = System.nanoTime();
        animator.ensureParallelPhase(-7L);
        long seed = animator.getParallelPhaseSeed();
        // Math.floorMod(-7, 10_000) = 9993, so the seed should be ~10 seconds
        // BEFORE now (within timing slack).
        long offsetNs = now - seed;
        assertLooselyInRange(offsetNs, 0L, 10_000L * 1_000_000L + 100_000_000L);
    }

    private static void assertLooselyInRange(long value, long lo, long hi) {
        if (value < lo || value > hi) {
            throw new AssertionError("Value " + value + " not in [" + lo + ", " + hi + "]");
        }
    }
}
