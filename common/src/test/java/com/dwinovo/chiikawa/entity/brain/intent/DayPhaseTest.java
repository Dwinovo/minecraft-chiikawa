package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class DayPhaseTest {
    @Test
    void phasesFollowTheTimeOfDay() {
        assertEquals(DayPhase.MORNING, DayPhase.of(0));
        assertEquals(DayPhase.MORNING, DayPhase.of(2999));
        assertEquals(DayPhase.DAY, DayPhase.of(3000));
        assertEquals(DayPhase.DAY, DayPhase.of(11999));
        assertEquals(DayPhase.EVENING, DayPhase.of(12000));
        assertEquals(DayPhase.EVENING, DayPhase.of(12999));
        assertEquals(DayPhase.NIGHT, DayPhase.of(13000));
        assertEquals(DayPhase.NIGHT, DayPhase.of(22999));
        assertEquals(DayPhase.MORNING, DayPhase.of(23000));
    }

    @Test
    void dayTimeWrapsAroundWholeDays() {
        assertEquals(DayPhase.NIGHT, DayPhase.of(5L * 24000 + 18000));
        assertEquals(DayPhase.MORNING, DayPhase.of(-1));
        assertEquals(DayPhase.NIGHT, DayPhase.of(-11000));
    }

    @Test
    void serializesByConstantName() {
        assertEquals(new JsonPrimitive("NIGHT"), DayPhase.CODEC.encodeStart(JsonOps.INSTANCE, DayPhase.NIGHT).getOrThrow());
        assertEquals(DayPhase.MORNING, DayPhase.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("MORNING")).getOrThrow());
    }
}
