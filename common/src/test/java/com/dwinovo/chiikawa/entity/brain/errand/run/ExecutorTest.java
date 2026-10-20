package com.dwinovo.chiikawa.entity.brain.errand.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.testing.FakeHandle;
import com.dwinovo.chiikawa.testing.TestRests;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** How an errand is run and, above all, how it ends: once, between steps, leaving nothing behind. */
class ExecutorTest {
    private static final ResourceLocation ERRAND = ResourceLocation.fromNamespaceAndPath("chiikawa", "work");
    private static final int DONE_REST = 100;
    private static final int GAVE_UP_REST = 200;
    private static final int INTERRUPTED_REST = 50;
    private static final int MAX_TICKS = 5;
    private static final Plan PLAN = new Plan() {
    };

    private final FakeHandle handle = new FakeHandle();
    private final Projected projection = new Projected();
    private final TestRests.Store store = new TestRests.Store();
    private final DecisionLog log = new DecisionLog();
    private final ErrandSettings<String> settings =
        TestRests.settings(MAX_TICKS, TestRests.ticks(DONE_REST, GAVE_UP_REST, INTERRUPTED_REST));
    private Executor executor;

    @BeforeEach
    void setUp() {
        executor = executorOver(store, log);
    }

    @Test
    void anEndAskedForWaitsForAStepThatCanBeCut() {
        ScriptedStep action = ScriptedStep.running();
        action.interruptible = false;
        start(Routine.single(action), 0);

        executor.requestEnd(Outcome.interrupted("higher"));
        executor.tick(1);
        executor.tick(2);

        assertTrue(executor.running().isPresent(), "a step that cannot be cut is finished first");
        assertEquals(0, action.released);

        action.interruptible = true;
        executor.tick(3);

        assertFalse(executor.running().isPresent());
        assertEquals(Outcome.interrupted("higher"), onlyEnding());
        assertEquals(1, action.released);
    }

    @Test
    void anEndFoundAtADecisionIsDeferredTheSameWay() {
        ScriptedStep action = ScriptedStep.running();
        action.interruptible = false;
        start(Routine.single(action), 0);

        assertFalse(executor.end(Outcome.gaveUp("gone"), 1));
        assertFalse(executor.interruptible());

        action.interruptible = true;
        assertTrue(executor.end(Outcome.gaveUp("gone"), 2), "now it may be ended");
        assertEquals(Outcome.gaveUp("gone"), onlyEnding());
    }

    @Test
    void theFirstEndAskedForStands() {
        start(Routine.single(ScriptedStep.running()), 0);

        executor.requestEnd(Outcome.interrupted("directive"));
        executor.requestEnd(Outcome.interrupted("higher"));
        executor.tick(1);

        assertEquals(Outcome.interrupted("directive"), onlyEnding());
    }

    @Test
    void anEndAskedForOfAPetOnNoErrandDoesNotEndTheNextOne() {
        executor.requestEnd(Outcome.interrupted("higher"));
        start(Routine.single(ScriptedStep.running()), 0);
        executor.tick(1);

        assertTrue(executor.running().isPresent());
    }

    @Test
    void anErrandThatRunsOutOfTimeGivesUp() {
        ScriptedStep step = ScriptedStep.running();
        start(Routine.single(step), 10);

        executor.tick(10 + MAX_TICKS - 1);
        assertTrue(executor.running().isPresent());

        executor.tick(10 + MAX_TICKS);

        assertEquals(Outcome.gaveUp("timeout"), onlyEnding());
        assertEquals(1, step.released);
        assertEquals(1, step.ticked, "the step is not ticked once the time is up");
    }

    @Test
    void theTimeLimitWaitsForAStepThatCanBeCutToo() {
        ScriptedStep action = ScriptedStep.running();
        action.interruptible = false;
        start(Routine.single(action), 0);

        executor.tick(MAX_TICKS + 3);
        assertTrue(executor.running().isPresent());

        action.interruptible = true;
        executor.tick(MAX_TICKS + 4);

        assertEquals(Outcome.gaveUp("timeout"), onlyEnding());
    }

    @Test
    void theErrandKeepsTheSettingsItStartedWith() {
        start(Routine.single(ScriptedStep.running()), 0);

        assertSame(settings, executor.running().orElseThrow().settings());
        assertEquals(50, executor.running().orElseThrow().priority());
    }

    @Test
    void everyKindOfEndReleasesTheStepOnceAndRestsOnceAsTheSettingsSay() {
        record Ending(Outcome outcome, int rest) {
        }
        for (Ending ending : new Ending[] {new Ending(Outcome.done(), DONE_REST),
                new Ending(Outcome.gaveUp("no"), GAVE_UP_REST), new Ending(Outcome.interrupted("moved"), INTERRUPTED_REST)}) {
            TestRests.Store fresh = new TestRests.Store();
            DecisionLog fresher = new DecisionLog();
            Executor each = executorOver(fresh, fresher);
            ScriptedStep step = ScriptedStep.running();
            each.start(ERRAND, PLAN, settings, Routine.single(step), 0);

            switch (ending.outcome()) {
                case Outcome.Done done -> step.then(StepStatus.DONE);
                case Outcome.GaveUp gaveUp -> step.then(StepStatus.gaveUp(gaveUp.reason()));
                case Outcome.Interrupted interrupted -> each.requestEnd(interrupted);
            }
            each.tick(1);
            each.tick(2);

            assertFalse(each.running().isPresent(), ending.outcome().toString());
            assertEquals(1, fresher.entries().size(), "logged once for " + ending.outcome());
            assertEquals(1, step.released, "released once for " + ending.outcome());
            assertEquals(1, fresh.writes(), "rested once for " + ending.outcome());
            assertEquals(ending.rest(), fresh.remaining(null, 1), "the rest for " + ending.outcome());
        }
    }

    @Test
    void theWalkAndTheGazeAreClearedOnceWhenTheErrandEnds() {
        start(Routine.single(ScriptedStep.done()), 0);
        executor.tick(1);

        assertEquals(1, handle.walkCleared);
        assertEquals(1, handle.lookCleared);
    }

    @Test
    void whatThePlayerIsToldIsPublishedWhileItRunsAndClearedWhenItEnds() {
        ScriptedStep step = ScriptedStep.running();
        step.pose = PetActivity.PHOTO;
        step.performance = "scene";
        start(Routine.single(step), 0);

        assertEquals(ERRAND, projection.intent);
        assertEquals(PetActivity.PHOTO, projection.activity);
        assertEquals("scene", projection.performance);

        step.pose = PetActivity.READ_RESULTS;
        executor.tick(1);
        assertEquals(PetActivity.READ_RESULTS, projection.activity, "the pose follows the step");

        executor.requestEnd(Outcome.interrupted("higher"));
        executor.tick(2);

        assertNull(projection.intent);
        assertEquals(PetActivity.NONE, projection.activity);
        assertEquals("", projection.performance);
    }

    @Test
    void aStepThatIsDoneHandsOverToTheNextAndTheLastEndsTheErrandDone() {
        ScriptedStep first = ScriptedStep.done();
        ScriptedStep second = ScriptedStep.running().then(StepStatus.RUNNING).then(StepStatus.DONE);
        start(Routine.of(Routine.step(() -> first), Routine.step(() -> second)), 0);

        assertEquals(1, first.begun);
        assertEquals(0, second.begun, "a step begins only when its turn comes");

        executor.tick(1);
        assertEquals(1, first.released, "a step that is over lets go of what it holds as the next begins");
        assertEquals(1, second.begun);
        assertEquals(1, executor.running().orElseThrow().stepIndex());
        assertEquals(0, handle.walkCleared, "the walk goes on from one step to the next");

        executor.tick(2);
        assertTrue(executor.running().isPresent());
        executor.tick(3);

        assertEquals(Outcome.done(), onlyEnding());
        assertEquals(1, first.released);
        assertEquals(1, second.released);
        assertEquals(DONE_REST, store.remaining(null, 3));
    }

    @Test
    void aStepThatCannotGoOnEndsTheErrandGivenUpWithItsReason() {
        ScriptedStep step = ScriptedStep.running().then(StepStatus.gaveUp("unreachable"));
        start(Routine.single(step), 0);

        executor.tick(1);

        assertEquals(Outcome.gaveUp("unreachable"), onlyEnding());
        assertEquals(1, step.released);
    }

    @Test
    void anErrandWithNoStepsIsDoneAtOnce() {
        start(Routine.of(), 0);

        assertFalse(executor.running().isPresent());
        assertEquals(Outcome.done(), onlyEnding());
    }

    @Test
    void aMessageGoesToTheStepAndWhatItSaysIsAsGoodAsATick() {
        ScriptedStep step = ScriptedStep.running();
        step.onMessage = StepStatus.DONE;
        start(Routine.single(step), 0);
        Message song = new Message.MusicStopped("over");

        executor.deliver(song, 1);

        assertSame(song, step.told);
        assertEquals(Outcome.done(), onlyEnding());
    }

    @Test
    void aMessageToAPetOnNoErrandGoesNowhere() {
        executor.deliver(new Message.MusicStopped("over"), 1);

        assertEquals(0, log.entries().size());
    }

    @Test
    void theTagsOfTheStepAreWhatTheRestOfTheGameSees() {
        ScriptedStep seated = ScriptedStep.running();
        seated.tags = Set.of(StepTag.SEATED);
        start(Routine.single(seated), 0);

        assertEquals(Set.of(StepTag.SEATED), executor.running().orElseThrow().tags());
    }

    @Test
    void anErrandCannotBeStartedOverAnother() {
        start(Routine.single(ScriptedStep.running()), 0);

        assertThrows(IllegalStateException.class, () -> start(Routine.single(ScriptedStep.running()), 1));
    }

    @Test
    void theLogHasHowLongItRanAndHowItEnded() {
        start(Routine.single(ScriptedStep.running().then(StepStatus.RUNNING).then(StepStatus.DONE)), 10);

        executor.tick(11);
        executor.tick(12);

        DecisionLog.Ended ended = (DecisionLog.Ended) log.entries().get(0);
        assertEquals(ERRAND, ended.errand());
        assertEquals(12L, ended.gameTime());
        assertEquals(2L, ended.ranTicks());
    }

    private Executor executorOver(TestRests.Store rests, DecisionLog decisions) {
        return new Executor(handle, projection, new RestLedger(errand -> rests, errand -> Optional.empty()), decisions);
    }

    private void start(Routine routine, long now) {
        executor.start(ERRAND, PLAN, settings, routine, now);
    }

    private Outcome onlyEnding() {
        assertEquals(1, log.entries().size(), "an errand ends once");
        return ((DecisionLog.Ended) log.entries().get(0)).outcome();
    }

    /** Remembers what the executor last published. */
    private static final class Projected implements Projection {
        @Nullable
        ResourceLocation intent;
        PetActivity activity = PetActivity.NONE;
        String performance = "";

        @Override
        public void intent(@Nullable ResourceLocation errand) {
            intent = errand;
        }

        @Override
        public void activity(PetActivity activity) {
            this.activity = activity;
        }

        @Override
        public void performance(String performance) {
            this.performance = performance;
        }
    }
}
