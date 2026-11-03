package com.dwinovo.chiikawa.anim.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationClockTest {
    @Test
    void convertsGameTicksToNanoseconds() {
        assertEquals(5_012_500_000L, AnimationClock.fromTicks(100, 0.25f));
    }

    @Test
    void samePausedTickProducesSameTime() {
        long paused = AnimationClock.fromTicks(240, 0.6f);
        assertEquals(paused, AnimationClock.fromTicks(240, 0.6f));
    }
}
