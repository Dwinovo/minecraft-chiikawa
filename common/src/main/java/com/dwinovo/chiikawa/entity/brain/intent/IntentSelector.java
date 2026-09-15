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
import java.util.List;
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
 * ends the running intent if it is no longer offered, permitted or able to continue,
 * then scores the generic intents plus those of the pet's capability and switches to
 * the best one.
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

        Evaluation evaluation = evaluate(pet);
        forgetUnavailable(brain, evaluation.candidates());
        RunningIntent current = running.orElse(null);
        Decision decision = choose(evaluation.candidates(), current,
            new SelectorParams(now, HOLD_BONUS, JITTER, MIN_DWELL_TICKS), pet.getRandom());
        if (decision.keeps(current)) {
            return;
        }
        PetIntent previous = current == null ? null : PetIntents.get(current.id());
        PetIntent next = decision.next() == null ? null : PetIntents.get(decision.next());
        String cause = decision.ended() != null ? decision.ended().name() : trigger.name();
        switchIntent(pet, level, previous, next, now, cause, decision.ranking());
    }

    /**
     * Snapshot of every candidate for the debug command.
     */
    public static Snapshot describe(AbstractPet pet) {
        Evaluation evaluation = evaluate(pet);
        List<CandidateView> views = new ArrayList<>();
        for (int i = 0; i < evaluation.candidates().size(); i++) {
            Candidate candidate = evaluation.candidates().get(i);
            views.add(new CandidateView(evaluation.intents().get(i), candidate.allowed(), candidate.check(), candidate.score()));
        }
        return new Snapshot(pet.getBrain().getMemory(InitMemory.CURRENT_INTENT.get()), pet.level().getGameTime(), views);
    }

    /**
     * Pure selection core.
     *
     * <ol>
     *   <li>The running intent ends if it is no longer offered or permitted, or its
     *       continue condition fails.</li>
     *   <li>Every permitted candidate whose condition holds is scored: base score plus
     *       noise, plus the hold bonus for the running intent. During the minimum dwell
     *       only candidates with a higher base score than the running intent compete,
     *       so noise cannot make a pet hop between intents while a genuinely more
     *       important one still takes over at once.</li>
     * </ol>
     *
     * @param candidates offered intents in a stable order; for the running intent the
     *                   check is its continue condition
     * @param current the running intent, or {@code null}
     * @param params tuning and the current game time
     * @param random noise source, consumed once per scored candidate in list order
     * @return the decision
     */
    static Decision choose(List<Candidate> candidates, @Nullable RunningIntent current, SelectorParams params, RandomSource random) {
        Candidate held = null;
        EndReason ended = null;
        if (current != null) {
            held = candidates.stream().filter(c -> c.id().equals(current.id())).findFirst().orElse(null);
            ended = endReason(held);
        }
        boolean dwelling = held != null && ended == null
            && params.gameTime() - current.startTick() < params.minDwellTicks();

        List<Scored> ranking = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean isHeld = candidate == held;
            if (isHeld ? ended != null : !candidate.eligible()) {
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

    private static @Nullable EndReason endReason(@Nullable Candidate held) {
        if (held == null) {
            return EndReason.NOT_OFFERED;
        }
        if (!held.allowed()) {
            return EndReason.NOT_ALLOWED;
        }
        if (!held.check().ok()) {
            return EndReason.CONDITION_FAILED;
        }
        return null;
    }

    private static Evaluation evaluate(AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        PetOwnership ownership = PetOwnership.of(pet);
        IntentContext ctx = IntentContext.capture(pet, ownership);
        Optional<RunningIntent> running = brain.getMemory(InitMemory.CURRENT_INTENT.get());

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
                isRunning ? intent.canContinue(ctx) : intent.canRun(ctx),
                intent.score(ctx)
            ));
        }
        return new Evaluation(offered, candidates);
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
            brain.setMemory(InitMemory.CURRENT_INTENT.get(), new RunningIntent(next.id(), now));
        } else {
            brain.eraseMemory(InitMemory.CURRENT_INTENT.get());
        }
        brain.getMemory(InitMemory.INTENT_SWITCH_LOG.get()).ifPresent(log -> log.record(
            now, previous == null ? null : previous.id(), next == null ? null : next.id(), cause, ranking));
    }

    private enum Trigger {
        NO_INTENT,
        PERIODIC,
        REQUESTED
    }

    enum EndReason {
        NOT_OFFERED,
        NOT_ALLOWED,
        CONDITION_FAILED
    }

    /**
     * @param allowed whether the directive permits the intent's category
     * @param check start condition, or continue condition for the running intent
     */
    record Candidate(ResourceLocation id, boolean allowed, IntentCheck check, float score) {
        boolean eligible() {
            return allowed && check.ok();
        }
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
        boolean keeps(@Nullable RunningIntent current) {
            return ended == null && Objects.equals(current == null ? null : current.id(), next);
        }
    }

    private record Evaluation(List<PetIntent> intents, List<Candidate> candidates) {
    }

    public record CandidateView(PetIntent intent, boolean allowed, IntentCheck check, float score) {
    }

    public record Snapshot(Optional<RunningIntent> running, long gameTime, List<CandidateView> candidates) {
    }
}
