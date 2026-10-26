package com.dwinovo.chiikawa.entity.brain.errand.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.overlay.Overlays;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.testing.FakeHandle;
import com.dwinovo.chiikawa.testing.StubErrand;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestRests;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** How an errand is run and, above all, how it ends: once, between steps, leaving nothing behind. */
class ExecutorTest {
    private static final ResourceLocation ERRAND = ResourceLocation.fromNamespaceAndPath("chiikawa", "work");
    private static final ErrandType<?, ?> TYPE = new StubErrand(ERRAND);
    private static final int DONE_REST = 100;
    private static final int GAVE_UP_REST = 200;
    private static final int INTERRUPTED_REST = 50;
    private static final int MAX_TICKS = 5;
    private static final Plan PLAN = new Plan() {
    };

    private final FakeHandle handle = new FakeHandle();
    private final Projected projection = new Projected();
    /** What the icon is worked out from besides the step, which a case sets. */
    private PetIcons.Signs signs = PetIcons.Signs.NONE;
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
    void aGlanceIsLeftOutWhileTheStepHoldsTheGazeAndMadeOtherwise() {
        executor.glance(null, 60);
        assertEquals(1, handle.glances, "a pet on no errand glances");

        ScriptedStep aiming = ScriptedStep.running();
        aiming.holdsGaze = true;
        start(Routine.single(aiming), 0);
        executor.glance(null, 60);
        assertEquals(1, handle.glances, "a step that sets the gaze every tick would overwrite it");

        executor.end(Outcome.interrupted("test"), 1);
        start(Routine.single(ScriptedStep.running()), 2);
        executor.glance(null, 60);
        assertEquals(2, handle.glances);
    }

    @Test
    void theIconIsWorkedOutFromTheStepAndWhatElseThePetIsAskedAndPublishedWhenItChanges() {
        assertEquals(PetIcon.NONE, projection.icon);

        ScriptedStep frightened = ScriptedStep.running();
        frightened.tags = Set.of(StepTag.SCARED);
        start(Routine.single(frightened), 0);
        assertEquals(PetIcon.SCARED, projection.icon, "a step tagged scared shows it as it begins");

        executor.end(Outcome.interrupted("test"), 1);
        assertEquals(PetIcon.NONE, projection.icon, "and no more once the step is over");
    }

    @Test
    void aPetOnNoErrandHasItsIconWorkedOutAsWellAtEveryTick() {
        signs = new PetIcons.Signs(true, false, Optional.empty(), DayPhase.DAY);
        executor.tick(1);
        assertEquals(PetIcon.GIFT, projection.icon, "a present is shown by a pet that is on no errand");

        signs = PetIcons.Signs.NONE;
        executor.tick(2);
        assertEquals(PetIcon.NONE, projection.icon);
    }

    @Test
    void anAsideSaysAboutSomethingAndMakesAMoveButDoesNotMoveThePet() {
        executor.aside().say(VoiceMoment.MISSING, Optional.of("tag.item.minecraft.arrows"));
        executor.aside().gesture("scratch_head");

        assertEquals(List.of(VoiceMoment.MISSING), handle.said);
        assertEquals(List.of(Optional.of("tag.item.minecraft.arrows")), handle.saidAbout);
        assertEquals(List.of("scratch_head"), handle.gestures);
        assertEquals(0, handle.walkCleared);
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
    void anErrandKeepsTheTypeItStartedAsWhateverIsReloadedAfterwards() {
        ErrandType<?, ?> reloaded = new StubErrand(ERRAND);
        start(Routine.single(ScriptedStep.running()), 0);

        assertSame(TYPE, executor.running().orElseThrow().type());
        assertEquals(ERRAND, executor.running().orElseThrow().errand());
        assertNotSame(reloaded, executor.running().orElseThrow().type(), "a reload makes new types, it does not change the one under way");
    }

    @Test
    void anErrandThatNoLongerExistsEndsLikeAnyOtherAndRestsNothing() {
        // As a pet is left by a data pack reload that took away the errand it was on.
        TestErrands.load();
        Executor orphaned = new Executor(handle, projection,
            new RestLedger(errand -> Errands.restsOf(brain(), errand), errand -> Optional.empty()), log, () -> signs);
        orphaned.start(TYPE, PLAN, settings, Routine.single(ScriptedStep.running()), 0);

        orphaned.requestEnd(Outcome.interrupted("higher"));
        orphaned.tick(1);

        assertFalse(orphaned.running().isPresent());
        assertEquals(Outcome.interrupted("higher"), onlyEnding());
        assertEquals(0L, Errands.restsOf(brain(), ERRAND).remaining(null, 1), "there is nothing for a rest to be kept in");
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
            each.start(TYPE, PLAN, settings, Routine.single(step), 0);

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
    void whatTheErrandHoldsAcrossItsStepsIsLetGoOfOnceHoweverItEndsAndAfterTheStep() {
        for (Outcome outcome : new Outcome[] {Outcome.done(), Outcome.gaveUp("no"), Outcome.interrupted("moved")}) {
            List<Integer> stepsReleased = new ArrayList<>();
            ScriptedStep step = ScriptedStep.running();
            Executor each = executorOver(new TestRests.Store(), new DecisionLog());
            each.start(TYPE, PLAN, settings, Routine.single(step).releasing(h -> stepsReleased.add(step.released)), 0);

            switch (outcome) {
                case Outcome.Done done -> step.then(StepStatus.DONE);
                case Outcome.GaveUp gaveUp -> step.then(StepStatus.gaveUp(gaveUp.reason()));
                case Outcome.Interrupted interrupted -> each.requestEnd(interrupted);
            }
            each.tick(1);
            each.tick(2);

            assertEquals(List.of(1), stepsReleased, "once, after the step, for " + outcome);
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

    @Test
    void anOverlayOfTheSettingsRunsBesideTheStepForAsLongAsTheErrandDoes() {
        ErrandSettings<String> chatting = new ErrandSettings<>(settings.priority(), settings.score(), MAX_TICKS, settings.rests(),
            List.of(), List.of(Overlays.IDLE_CHATTER), Optional.empty(), "");
        ScriptedStep step = ScriptedStep.running().then(StepStatus.RUNNING).then(StepStatus.RUNNING).then(StepStatus.DONE);
        executor.start(TYPE, PLAN, chatting, Routine.single(step), 0);

        executor.tick(1);
        executor.tick(2);
        executor.tick(3);

        assertEquals(List.of(VoiceMoment.IDLE, VoiceMoment.IDLE), handle.said, "once for each tick the errand goes on, not the one it ends in");
        assertFalse(executor.running().isPresent());

        executor.tick(4);

        assertEquals(2, handle.said.size(), "a pet on no errand has no overlay");
    }

    @Test
    void anErrandWithNoOverlaysSaysNothingOnTheSide() {
        start(Routine.single(ScriptedStep.running()), 0);

        executor.tick(1);
        executor.tick(2);

        assertEquals(List.of(), handle.said);
    }

    private static Brain<?> brain() {
        CompoundTag saved = new CompoundTag();
        saved.put("memories", new CompoundTag());
        return Brain.<LivingEntity>provider(List.of(), List.of()).makeBrain(new Dynamic<>(NbtOps.INSTANCE, saved));
    }

    private Executor executorOver(TestRests.Store rests, DecisionLog decisions) {
        return new Executor(handle, projection, new RestLedger(errand -> rests, errand -> Optional.empty()), decisions, () -> signs);
    }

    private void start(Routine routine, long now) {
        executor.start(TYPE, PLAN, settings, routine, now);
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
        PetIcon icon = PetIcon.NONE;

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

        @Override
        public void icon(PetIcon icon) {
            this.icon = icon;
        }
    }
}
