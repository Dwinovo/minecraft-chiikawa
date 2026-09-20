package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
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
 * then scores the generic intents plus those of the pet's capability, weighted by the
 * pet's personality and the time of day, and switches to the best one.
 */
public final class IntentSelector {
    public static final int EVAL_INTERVAL = 10;
    /**
     * How much better than the running intent another candidate must score before the
     * pet's randomness is added, so a score that drifts (following grows with distance)
     * does not flip the pet back and forth at a crossover.
     */
    static final float HOLD_MARGIN = 0.02F;
    /** Added to the base score of an intent whose work the pet's slip counts. */
    static final float TASK_BONUS = 0.2F;

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
            new SelectorParams(HOLD_MARGIN, evaluation.context().personality().randomness()), pet.getRandom());
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
        return new Snapshot(pet.getBrain().getMemory(InitMemory.CURRENT_INTENT.get()), pet.level().getGameTime(),
            evaluation.context().phase(), views);
    }

    /**
     * Pure selection core.
     *
     * <ol>
     *   <li>The running intent ends if it is no longer offered or permitted, or its
     *       continue condition fails.</li>
     *   <li>Among the permitted candidates whose condition holds, those scoring within
     *       the tolerance ({@code holdMargin + randomness}) of the best are acceptable.</li>
     *   <li>A running intent that is still acceptable keeps running. Otherwise the
     *       acceptable candidates are ranked by score plus noise of up to
     *       {@code randomness}, and the best one runs.</li>
     * </ol>
     *
     * <p>So randomness only decides between options the pet finds about as good, and
     * never makes it drop what it is doing: to take over, a candidate must beat the
     * running intent by more than the tolerance, and the running intent can then not
     * take over back. However random the personality, a pet cannot flip between two
     * intents, while one that is clearly more important still takes over at once.
     *
     * @param candidates offered intents in a stable order, scores already weighted by
     *                   personality; for the running intent the check is its continue
     *                   condition
     * @param current the running intent, or {@code null}
     * @param params tolerance and noise
     * @param random noise source, consumed once per ranked candidate in list order
     * @return the decision
     */
    static Decision choose(List<Candidate> candidates, @Nullable RunningIntent current, SelectorParams params, RandomSource random) {
        Candidate held = null;
        EndReason ended = null;
        if (current != null) {
            held = candidates.stream().filter(c -> c.id().equals(current.id())).findFirst().orElse(null);
            ended = endReason(held);
        }
        float best = Float.NEGATIVE_INFINITY;
        for (Candidate candidate : candidates) {
            if (candidate.eligible()) {
                best = Math.max(best, candidate.score());
            }
        }
        float acceptable = best - (params.holdMargin() + params.randomness());
        if (held != null && ended == null && held.score() >= acceptable) {
            return new Decision(held.id(), null, List.of());
        }

        List<Scored> ranking = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.eligible() && candidate.score() >= acceptable) {
                float noise = params.randomness() * (random.nextFloat() * 2.0F - 1.0F);
                ranking.add(new Scored(candidate.id(), candidate.score() + noise));
            }
        }
        // Stable sort: on equal scores the earlier candidate wins.
        ranking.sort(Comparator.comparingDouble((Scored scored) -> scored.score()).reversed());
        return new Decision(ranking.isEmpty() ? null : ranking.get(0).id(), ended, List.copyOf(ranking));
    }

    /**
     * Candidates as the selector scores them: whether the directive permits each intent,
     * its start condition (continue condition for the running one), and its base score,
     * raised by {@link #TASK_BONUS} when the pet's slip counts the intent's work, weighted
     * by the personality for the current part of the day.
     *
     * @param offered intents in a stable order
     * @param ctx the evaluation snapshot
     * @param running id of the running intent, or {@code null}
     * @param allows the directive's permission table
     */
    static List<Candidate> candidates(List<PetIntent> offered, IntentContext ctx, @Nullable ResourceLocation running,
            Predicate<IntentCategory> allows) {
        List<Candidate> candidates = new ArrayList<>(offered.size());
        for (PetIntent intent : offered) {
            candidates.add(new Candidate(
                intent.id(),
                allows.test(intent.category()),
                intent.id().equals(running) ? intent.canContinue(ctx) : intent.canRun(ctx),
                (intent.score(ctx) + (carriesSlipFor(intent, ctx) ? TASK_BONUS : 0.0F))
                    * ctx.personality().multiplier(intent.id(), ctx.phase())
            ));
        }
        return candidates;
    }

    private static boolean carriesSlipFor(PetIntent intent, IntentContext ctx) {
        return intent.workCounter().isPresent() && ctx.task().map(PetTask::counter).equals(intent.workCounter());
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
        PetOwnership ownership = PetOwnership.of(pet);
        IntentContext ctx = IntentContext.capture(pet, ownership);
        List<PetIntent> offered = new ArrayList<>(PetIntents.GENERIC);
        for (ResourceLocation id : InitRegistry.getCapabilityFromId(pet.getPetJobId()).intents()) {
            offered.add(PetIntents.get(id));
        }
        ResourceLocation running = pet.getBrain().getMemory(InitMemory.CURRENT_INTENT.get()).map(RunningIntent::id).orElse(null);
        return new Evaluation(ctx, offered,
            candidates(offered, ctx, running, category -> PetConstraints.allows(pet, ownership, category)));
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
        pet.setIntent(next == null ? null : next.id());
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
     * @param score base score weighted by personality
     */
    record Candidate(ResourceLocation id, boolean allowed, IntentCheck check, float score) {
        boolean eligible() {
            return allowed && check.ok();
        }
    }

    /**
     * @param holdMargin {@link #HOLD_MARGIN}
     * @param randomness the pet's personality randomness
     */
    record SelectorParams(float holdMargin, float randomness) {
    }

    public record Scored(ResourceLocation id, float score) {
    }

    /**
     * @param next the intent to run, {@code null} if none can
     * @param ended why the running intent ended, {@code null} if it did not
     * @param ranking acceptable candidates with their noisy scores, best first; empty
     *                when the running intent is kept
     */
    record Decision(@Nullable ResourceLocation next, @Nullable EndReason ended, List<Scored> ranking) {
        boolean keeps(@Nullable RunningIntent current) {
            return ended == null && Objects.equals(current == null ? null : current.id(), next);
        }
    }

    private record Evaluation(IntentContext context, List<PetIntent> intents, List<Candidate> candidates) {
    }

    /**
     * @param score base score weighted by personality
     */
    public record CandidateView(PetIntent intent, boolean allowed, IntentCheck check, float score) {
    }

    public record Snapshot(Optional<RunningIntent> running, long gameTime, DayPhase phase, List<CandidateView> candidates) {
    }
}
