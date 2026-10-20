package com.dwinovo.chiikawa.entity.brain.errand.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.testing.FakeHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The order a routine gives its steps in, and when each is made. */
class RoutineTest {
    private final FakeHandle handle = new FakeHandle();

    @Test
    void stepsComeOneAfterTheOtherAndThenNothing() {
        ScriptedStep first = ScriptedStep.running();
        ScriptedStep second = ScriptedStep.running();
        Routine routine = Routine.of(Routine.step(() -> first), Routine.step(() -> second));

        assertSame(first, routine.next(handle).orElseThrow());
        assertSame(second, routine.next(handle).orElseThrow());
        assertEquals(Optional.empty(), routine.next(handle));
        assertEquals(Optional.empty(), routine.next(handle));
    }

    @Test
    void aStepIsMadeOnlyWhenItsTurnComes() {
        List<String> made = new ArrayList<>();
        Routine routine = Routine.of(Routine.step(() -> {
            made.add("first");
            return ScriptedStep.running();
        }), Routine.step(() -> {
            made.add("second");
            return ScriptedStep.running();
        }));

        routine.next(handle);

        assertEquals(List.of("first"), made);
    }

    @Test
    void aRoundIsGoneRoundForAsLongAsTheConditionHoldsAndAskedBeforeTheFirst() {
        int[] rounds = {2};
        List<String> made = new ArrayList<>();
        Routine routine = Routine.of(
            Routine.repeatWhile(h -> rounds[0]-- > 0, () -> label(made, "walk"), () -> label(made, "deposit")),
            Routine.step(() -> label(made, "close")));

        while (routine.next(handle).isPresent()) {
            // Taking every step there is.
        }

        assertEquals(List.of("walk", "deposit", "walk", "deposit", "close"), made);
    }

    @Test
    void aConditionThatNeverHoldsGivesNoStepsAtAll() {
        Routine routine = Routine.of(Routine.repeatWhile(h -> false, ScriptedStep::running), Routine.step(ScriptedStep::running));

        assertTrue(routine.next(handle).isPresent(), "the step after the round");
        assertEquals(Optional.empty(), routine.next(handle));
    }

    @Test
    void theConditionIsAskedOnlyBetweenRounds() {
        int[] asked = {0};
        Routine routine = Routine.of(Routine.repeatWhile(h -> asked[0]++ < 1, ScriptedStep::running, ScriptedStep::running));

        routine.next(handle);
        routine.next(handle);
        assertEquals(1, asked[0], "not between the steps of a round");

        assertEquals(Optional.empty(), routine.next(handle));
        assertEquals(2, asked[0]);
        assertEquals(Optional.empty(), routine.next(handle));
        assertEquals(2, asked[0], "and not again once it has been told no");
    }

    @Test
    void aRoundOfNothingIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Routine.repeatWhile(h -> true));
    }

    private static ScriptedStep label(List<String> made, String name) {
        made.add(name);
        return ScriptedStep.running();
    }
}
