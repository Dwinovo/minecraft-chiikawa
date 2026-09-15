package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jetbrains.annotations.Nullable;

/**
 * Decides what a pet does, and is the only place that switches brain activities.
 *
 * <p>Runs before the brain ticks. Every {@link #EVAL_INTERVAL} ticks (staggered by
 * entity id), right after {@link #requestReevaluate}, or whenever no intent runs, it
 * ends the running intent if it is no longer offered, permitted, able to continue or
 * within its step time, then scores the generic intents plus those of the pet's
 * capability and switches to the best one.
 */
public final class IntentSelector {
    public static final int EVAL_INTERVAL = 10;
    public static final int MIN_DWELL_TICKS = 40;
    /**
     * Bonus for keeping the running intent, and the most that random noise adds or
     * removes. {@code HOLD_BONUS + 2 * JITTER} stays below 0.05, the smallest gap
     * between base scores that encodes a strict priority (harvest over plant over
     * deliver), so neither can reorder those.
     */
    static final float HOLD_BONUS = 0.02F;
    static final float JITTER = 0.01F;

    private IntentSelector() {
    }

    /** Makes the selector evaluate on the pet's next tick. */
    public static void requestReevaluate(AbstractPet pet) {
        pet.getBrain().setMemory(InitMemory.INTENT_REEVALUATE.get(), Unit.INSTANCE);
    }

    /**
     * Moves the running intent to its next step; completing the last step ends the
     * intent and starts its success cooldown.
     */
    public static void advanceStep(AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        brain.getMemory(InitMemory.CURRENT_INTENT.get()).ifPresent(running -> {
            PetIntent intent = PetIntents.get(running.id());
            if (intent == null || running.step() >= intent.steps().size()) {
                return;
            }
            long now = pet.level().getGameTime();
            RunningIntent next = running.nextStep(now);
            brain.setMemory(InitMemory.CURRENT_INTENT.get(), next);
            if (next.step() == intent.steps().size()) {
                startCooldown(brain, intent.id(), now, intent.successCooldown());
                requestReevaluate(pet);
            }
        });
    }

    public static void tick(AbstractPet pet, ServerLevel level) {
        Brain<AbstractPet> brain = pet.getBrain();
        long now = level.getGameTime();
        Optional<RunningIntent> running = brain.getMemory(InitMemory.CURRENT_INTENT.get());
        Trigger trigger;
        if (running.isEmpty()) {
            trigger = Trigger.NO_INTENT;
        } else if (brain.hasMemoryValue(InitMemory.INTENT_REEVALUATE.get())) {
            trigger = Trigger.REQUESTED;
        } else if ((now + pet.getId()) % EVAL_INTERVAL == 0) {
            trigger = Trigger.PERIODIC;
        } else {
            return;
        }
        brain.eraseMemory(InitMemory.INTENT_REEVALUATE.get());
        PetTargeting.clearInvalidAttackTarget(pet, brain);

        Evaluation evaluation = evaluate(pet, now);
        forgetUnavailable(brain, evaluation.candidates());
        Decision decision = choose(evaluation.candidates(), evaluation.current(),
            new SelectorParams(now, HOLD_BONUS, JITTER, MIN_DWELL_TICKS), pet.getRandom());
        if (decision.keeps(evaluation.current())) {
            return;
        }
        PetIntent previous = running.map(r -> PetIntents.get(r.id())).orElse(null);
        if (previous != null && decision.ended() != null && decision.ended() != EndReason.COMPLETED) {
            startCooldown(brain, previous.id(), now, previous.failureCooldown());
        }
        PetIntent next = decision.next() == null ? null : PetIntents.get(decision.next());
        String cause = decision.ended() != null ? decision.ended().name() : trigger.name();
        switchIntent(pet, level, previous, next, now, cause, decision.ranking());
    }

    /**
     * Snapshot of every candidate for the debug command.
     */
    public static Snapshot describe(AbstractPet pet) {
        long now = pet.level().getGameTime();
        Evaluation evaluation = evaluate(pet, now);
        List<CandidateView> views = new ArrayList<>();
        for (int i = 0; i < evaluation.candidates().size(); i++) {
            Candidate candidate = evaluation.candidates().get(i);
            views.add(new CandidateView(evaluation.intents().get(i), candidate.allowed(),
                Math.max(0L, candidate.cooldownUntil() - now), candidate.check(), candidate.score()));
        }
        return new Snapshot(pet.getBrain().getMemory(InitMemory.CURRENT_INTENT.get()), now, views);
    }

    /**
     * Pure selection core.
     *
     * <ol>
     *   <li>The running intent ends if it is no longer offered or permitted, its last
     *       step is done, its condition fails, or its step timed out.</li>
     *   <li>A running intent in an uninterruptible step is kept.</li>
     *   <li>Otherwise every permitted, off-cooldown candidate whose condition holds is
     *       scored: base score plus noise, plus the hold bonus for the running intent.
     *       During the minimum dwell only candidates with a higher base score than the
     *       running intent compete, so noise cannot make a pet hop between intents
     *       while a genuinely more important one still takes over at once.</li>
     * </ol>
     *
     * @param candidates offered intents in a stable order; for the running intent the
     *                   check is its continue condition
     * @param current the running intent, or {@code null}
     * @param params tuning and the current game time
     * @param random noise source, consumed once per scored candidate in list order
     * @return the decision
     */
    static Decision choose(List<Candidate> candidates, @Nullable Running current, SelectorParams params, RandomSource random) {
        long now = params.gameTime();
        Candidate held = null;
        EndReason ended = null;
        if (current != null) {
            held = candidates.stream().filter(c -> c.id().equals(current.id())).findFirst().orElse(null);
            ended = endReason(current, held, now);
            if (ended == null && current.step() != null && !current.step().interruptible()) {
                return new Decision(current.id(), null, List.of());
            }
        }
        boolean dwelling = held != null && ended == null && now - current.startTick() < params.minDwellTicks();

        List<Scored> ranking = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean isHeld = candidate == held;
            if (isHeld ? ended != null : !candidate.eligibleAt(now)) {
                continue;
            }
            if (dwelling && !isHeld && candidate.score() <= held.score()) {
                continue;
            }
            float score = candidate.score() + params.jitter() * (random.nextFloat() * 2.0F - 1.0F);
            if (isHeld) {
                score += params.holdBonus();
            }
            ranking.add(new Scored(candidate.id(), score));
        }
        // Stable sort: on equal scores the earlier candidate wins.
        ranking.sort(Comparator.comparingDouble((Scored scored) -> scored.score()).reversed());
        return new Decision(ranking.isEmpty() ? null : ranking.get(0).id(), ended, List.copyOf(ranking));
    }

    private static @Nullable EndReason endReason(Running current, @Nullable Candidate held, long now) {
        if (held == null) {
            return EndReason.NOT_OFFERED;
        }
        if (!held.allowed()) {
            return EndReason.NOT_ALLOWED;
        }
        if (current.completed()) {
            return EndReason.COMPLETED;
        }
        if (!held.check().ok()) {
            return EndReason.CONDITION_FAILED;
        }
        IntentStep step = current.step();
        if (step != null && step.timeoutTicks() > 0 && now - current.stepStartTick() >= step.timeoutTicks()) {
            return EndReason.STEP_TIMEOUT;
        }
        return null;
    }

    private static Evaluation evaluate(AbstractPet pet, long now) {
        Brain<AbstractPet> brain = pet.getBrain();
        PetOwnership ownership = PetOwnership.of(pet);
        IntentContext ctx = IntentContext.capture(pet, ownership);
        Optional<RunningIntent> running = brain.getMemory(InitMemory.CURRENT_INTENT.get());
        Map<ResourceLocation, Long> cooldowns = brain.getMemory(InitMemory.INTENT_COOLDOWNS.get()).orElse(Map.of());

        List<PetIntent> offered = new ArrayList<>(PetIntents.GENERIC);
        for (ResourceLocation id : InitRegistry.getCapabilityFromId(pet.getPetJobId()).intents()) {
            offered.add(PetIntents.get(id));
        }
        List<Candidate> candidates = new ArrayList<>(offered.size());
        for (PetIntent intent : offered) {
            boolean isRunning = running.filter(r -> r.id().equals(intent.id())).isPresent();
            candidates.add(new Candidate(
                intent.id(),
                PetConstraints.allows(pet, ownership, intent.category()),
                cooldowns.getOrDefault(intent.id(), 0L),
                isRunning ? intent.canContinue(ctx) : intent.canRun(ctx),
                intent.score(ctx)
            ));
        }
        Running current = running.map(r -> toRunning(r, PetIntents.get(r.id()))).orElse(null);
        return new Evaluation(offered, candidates, current);
    }

    private static @Nullable Running toRunning(RunningIntent running, @Nullable PetIntent intent) {
        if (intent == null) {
            return null;
        }
        List<IntentStep> steps = intent.steps();
        IntentStep step = running.step() < steps.size() ? steps.get(running.step()) : null;
        return new Running(running.id(), running.startTick(), step, running.stepStartTick(),
            !steps.isEmpty() && running.step() >= steps.size());
    }

    /** Erases the progress memories of intents the pet can currently not pursue. */
    private static void forgetUnavailable(Brain<AbstractPet> brain, List<Candidate> candidates) {
        for (PetIntent intent : PetIntents.all()) {
            if (intent.forgetWhenUnavailable().isEmpty()) {
                continue;
            }
            boolean available = candidates.stream().anyMatch(c -> c.id().equals(intent.id()) && c.allowed());
            if (!available) {
                intent.forgetWhenUnavailable().forEach(brain::eraseMemory);
            }
        }
    }

    /**
     * Switch-out order: the previous intent's {@link PetIntent#onStop}, then the new
     * activity (vanilla erases the memories the previous activity declared), then the
     * previous activity's running behaviors are stopped, and movement and gaze are
     * reset. Vanilla keeps ticking running behaviors after an activity change, so
     * they must be stopped here; erasing the declared memories first means an
     * interrupted behavior never acts on an abandoned target, e.g. an archer told to
     * sit does not release its drawn arrow.
     */
    private static void switchIntent(AbstractPet pet, ServerLevel level, @Nullable PetIntent previous,
            @Nullable PetIntent next, long now, String cause, List<Scored> ranking) {
        Brain<AbstractPet> brain = pet.getBrain();
        if (previous != null) {
            previous.onStop(new IntentRuntime(pet, level, brain));
        }
        if (next != null) {
            brain.setActiveActivityIfPossible(next.activity());
        } else {
            brain.useDefaultActivity();
        }
        if (previous != null) {
            for (BehaviorControl<? super AbstractPet> behavior : PetActivities.behaviorsOf(brain, previous.activity())) {
                if (behavior.getStatus() == Behavior.Status.RUNNING) {
                    behavior.doStop(level, pet, now);
                }
            }
        }
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.PATH);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        if (next != null) {
            brain.setMemory(InitMemory.CURRENT_INTENT.get(), RunningIntent.start(next.id(), now));
        } else {
            brain.eraseMemory(InitMemory.CURRENT_INTENT.get());
        }
        brain.getMemory(InitMemory.INTENT_SWITCH_LOG.get()).ifPresent(log -> log.record(
            now, previous == null ? null : previous.id(), next == null ? null : next.id(), cause, ranking));
    }

    private static void startCooldown(Brain<AbstractPet> brain, ResourceLocation id, long now, int ticks) {
        Map<ResourceLocation, Long> cooldowns = new HashMap<>();
        brain.getMemory(InitMemory.INTENT_COOLDOWNS.get()).ifPresent(existing ->
            existing.forEach((key, until) -> {
                if (until > now) {
                    cooldowns.put(key, until);
                }
            }));
        if (ticks > 0) {
            cooldowns.put(id, now + ticks);
        }
        if (cooldowns.isEmpty()) {
            brain.eraseMemory(InitMemory.INTENT_COOLDOWNS.get());
        } else {
            brain.setMemory(InitMemory.INTENT_COOLDOWNS.get(), Map.copyOf(cooldowns));
        }
    }

    private enum Trigger {
        NO_INTENT,
        PERIODIC,
        REQUESTED
    }

    enum EndReason {
        NOT_OFFERED,
        NOT_ALLOWED,
        COMPLETED,
        CONDITION_FAILED,
        STEP_TIMEOUT
    }

    /**
     * @param allowed whether the directive permits the intent's category
     * @param cooldownUntil game time the intent's cooldown ends
     * @param check start condition, or continue condition for the running intent
     */
    record Candidate(ResourceLocation id, boolean allowed, long cooldownUntil, IntentCheck check, float score) {
        boolean eligibleAt(long gameTime) {
            return allowed && cooldownUntil <= gameTime && check.ok();
        }
    }

    /**
     * @param step the current step, {@code null} for step-less intents or once all steps are done
     * @param completed whether the intent finished its last step
     */
    record Running(ResourceLocation id, long startTick, @Nullable IntentStep step, long stepStartTick, boolean completed) {
    }

    record SelectorParams(long gameTime, float holdBonus, float jitter, int minDwellTicks) {
    }

    public record Scored(ResourceLocation id, float score) {
    }

    /**
     * @param next the intent to run, {@code null} if none can
     * @param ended why the running intent ended, {@code null} if it did not
     * @param ranking scored candidates, best first
     */
    record Decision(@Nullable ResourceLocation next, @Nullable EndReason ended, List<Scored> ranking) {
        boolean keeps(@Nullable Running current) {
            return ended == null && Objects.equals(current == null ? null : current.id(), next);
        }
    }

    private record Evaluation(List<PetIntent> intents, List<Candidate> candidates, @Nullable Running current) {
    }

    public record CandidateView(PetIntent intent, boolean allowed, long cooldownRemaining, IntentCheck check, float score) {
    }

    public record Snapshot(Optional<RunningIntent> running, long gameTime, List<CandidateView> candidates) {
    }
}
