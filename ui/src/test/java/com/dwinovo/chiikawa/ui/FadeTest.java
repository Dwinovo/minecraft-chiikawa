package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FadeTest {
    @Test
    void itFadesInOverItsOwnTime() {
        Fade half = Fade.HIDDEN.step(true, Fade.IN_SECONDS / 2);

        assertEquals(0.5F, half.alpha(), 1.0E-4F);
        assertEquals(1.0F, half.step(true, Fade.IN_SECONDS).alpha(), 1.0E-4F);
    }

    @Test
    void itHoldsBeforeItLeaves() {
        Fade shown = Fade.HIDDEN.step(true, Fade.IN_SECONDS);

        Fade grazed = shown.step(false, Fade.HOLD_SECONDS / 2);
        assertEquals(1.0F, grazed.alpha(), 1.0E-4F);
        assertTrue(grazed.isVisible());
    }

    @Test
    void aGrazeThatComesBackNeverDips() {
        Fade shown = Fade.HIDDEN.step(true, Fade.IN_SECONDS);

        Fade back = shown.step(false, Fade.HOLD_SECONDS / 2).step(true, 0.01F);
        assertEquals(1.0F, back.alpha(), 1.0E-4F);
        assertEquals(Fade.HOLD_SECONDS, back.holdLeft(), 1.0E-4F);
    }

    @Test
    void itFadesOutOnceTheHoldIsUsedUp() {
        Fade shown = Fade.HIDDEN.step(true, Fade.IN_SECONDS);

        Fade going = shown.step(false, Fade.HOLD_SECONDS + Fade.OUT_SECONDS / 2);
        assertEquals(0.5F, going.alpha(), 1.0E-4F);
        assertFalse(going.step(false, Fade.OUT_SECONDS).isVisible());
    }

    @Test
    void oneLongFrameCannotHideInsideTheHold() {
        Fade shown = Fade.HIDDEN.step(true, Fade.IN_SECONDS);

        // A stutter longer than the hold plus the fade lands at nothing, not at "still holding".
        assertFalse(shown.step(false, 5.0F).isVisible());
    }

    @Test
    void aFadeOutInterruptedRisesFromWhereItGotTo() {
        Fade going = Fade.HIDDEN.step(true, Fade.IN_SECONDS).step(false, Fade.HOLD_SECONDS + Fade.OUT_SECONDS / 2);

        assertEquals(1.0F, going.step(true, Fade.IN_SECONDS / 2).alpha(), 1.0E-4F);
    }
}
