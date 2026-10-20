package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.overlay.Overlays;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the one errand a pet is on, step by step, and ends it. The only thing that moves the pet's
 * walk, gaze and path, and the only thing that sets what the player's game is told it is doing.
 * One for each pet.
 *
 * <p>An errand is ended between steps and never in the middle of one that cannot be cut:
 * {@link #requestEnd} and the time limit wait for a step that is {@link Step#interruptible}, so a
 * half-done action is finished and the longest an end waits is one tick. However an errand ends it
 * ends once, in this order: the step lets go of what it holds, the routine lets go of what the errand holds
 * across its steps, the walk, gaze and path are cleared, the rest is written, the end is logged, and what the
 * player is told is cleared with it.
 *
 * <p>Beside the step, the {@link Overlay}s the errand's settings name are ticked, with an {@link Aside} that says and
 * glances but cannot walk.
 */
public final class Executor {
    private final Handle handle;
    private final Projection projection;
    private final RestLedger rests;
    private final DecisionLog log;
    private final Aside aside = new Aside() {
        @Override
        public void say(VoiceMoment moment) {
            handle.say(moment);
        }

        @Override
        public void glance(Entity target, int ticks) {
            Executor.this.glance(target, ticks);
        }
    };

    @Nullable
    private Running running;
    @Nullable
    private Routine routine;
    @Nullable
    private Step step;
    /** How the errand is to end at the next step it may be cut at, if something has said so. */
    @Nullable
    private Outcome pending;
    private int stepsBegun;
    /** The pose and the scene last published, so that only a change is: see {@link #project}. */
    private PetActivity projectedActivity = PetActivity.NONE;
    private String projectedPerformance = "";

    /**
     * @param handle the pet
     * @param projection where to publish what the pet is doing
     * @param rests where an errand's rest is written when it ends
     * @param log where an errand's end is recorded
     */
    public Executor(Handle handle, Projection projection, RestLedger rests, DecisionLog log) {
        this.handle = handle;
        this.projection = projection;
        this.rests = rests;
        this.log = log;
    }

    /** @return the errand the pet is on */
    public Optional<Running> running() {
        return Optional.ofNullable(running);
    }

    /** @return whether the pet is on no errand, or on one that may be ended now */
    public boolean interruptible() {
        return step == null || step.interruptible();
    }

    /**
     * Sets off on an errand.
     *
     * @param errand its id
     * @param plan what the pet means to do
     * @param settings how it goes; the errand keeps these however the data is reloaded
     * @param routine its steps, made for this start and not used for another
     * @param now the current game time
     * @throws IllegalStateException if the pet is still on another errand
     */
    public void start(ResourceLocation errand, Plan plan, ErrandSettings<?> settings, Routine routine, long now) {
        if (running != null) {
            throw new IllegalStateException("Still on " + running.errand() + ", end it before starting " + errand);
        }
        this.running = new Running(errand, plan, settings, 0, now, Set.of());
        this.routine = routine;
        this.pending = null;
        this.stepsBegun = 0;
        advance(now);
        project();
    }

    /**
     * Asks for the errand to end the way it says, at the next step it may be cut at. The first thing
     * asked for stands: it is what really took the pet away. Nothing is asked of a pet on no errand.
     */
    public void requestEnd(Outcome outcome) {
        if (running != null && pending == null) {
            pending = outcome;
        }
    }

    /**
     * Ends the errand now if the step it is at may be cut, and otherwise asks for it to end at the
     * next one that may.
     *
     * @return whether the pet is now on no errand
     */
    public boolean end(Outcome outcome, long now) {
        requestEnd(outcome);
        applyPending(now);
        project();
        return running == null;
    }

    /** One tick of the errand: the time limit, an end that was asked for, then the step and the overlays beside it. */
    public void tick(long now) {
        if (running == null) {
            return;
        }
        if (now - running.startedAt() >= running.settings().maxTicks()) {
            requestEnd(Outcome.gaveUp("timeout"));
        }
        if (!applyPending(now)) {
            apply(step.tick(handle), now);
            tickOverlays();
        }
        project();
    }

    /**
     * A passing look at something, for as long as given: unless the step the pet is at holds its gaze
     * itself, which would overwrite it in the next tick. Reaches a pet on no errand as well.
     */
    public void glance(Entity target, int ticks) {
        if (step == null || !step.holdsGaze()) {
            handle.lookAt(target, ticks);
        }
    }

    /** Tells the step the pet is at something that concerns its errand. */
    public void deliver(Message message, long now) {
        if (step != null) {
            apply(step.onMessage(handle, message), now);
            project();
        }
    }

    /** Ticks the overlays of the errand the pet is still on, if the step did not just end it. */
    private void tickOverlays() {
        if (running != null) {
            running.settings().overlays().forEach(id -> Overlays.get(id).ifPresent(overlay -> overlay.tick(aside)));
        }
    }

    private boolean applyPending(long now) {
        if (pending == null || !step.interruptible()) {
            return false;
        }
        finish(pending, now);
        return true;
    }

    private void apply(StepStatus status, long now) {
        switch (status.kind()) {
            case RUNNING -> {
            }
            case DONE -> advance(now);
            case GAVE_UP -> finish(Outcome.gaveUp(status.reason()), now);
        }
    }

    /** Goes on to the next step, or ends the errand done if there is none. */
    private void advance(long now) {
        releaseStep();
        Optional<Step> next = routine.next(handle);
        if (next.isEmpty()) {
            finish(Outcome.done(), now);
            return;
        }
        step = next.get();
        running = running.at(stepsBegun++, step.tags());
        step.begin(handle);
    }

    private void releaseStep() {
        if (step != null) {
            Step ended = step;
            step = null;
            ended.release(handle);
        }
    }

    private void finish(Outcome outcome, long now) {
        Running ended = running;
        releaseStep();
        routine.release(handle);
        handle.stopWalking();
        handle.clearLook();
        rests.afterOutcome(ended, outcome, now);
        log.ended(now, ended.errand(), outcome, now - ended.startedAt());
        running = null;
        routine = null;
        pending = null;
    }

    /**
     * Publishes what the pet is doing. The errand is published as it is; the pose and the scene only when
     * what the step claims of them changes, and they are cleared the tick the step that claimed them ends.
     */
    private void project() {
        projection.intent(running == null ? null : running.errand());
        PetActivity pose = step == null ? PetActivity.NONE : step.pose();
        if (pose != projectedActivity) {
            projectedActivity = pose;
            projection.activity(pose);
        }
        String performance = step == null ? "" : step.performance();
        if (!performance.equals(projectedPerformance)) {
            projectedPerformance = performance;
            projection.performance(performance);
        }
    }
}
