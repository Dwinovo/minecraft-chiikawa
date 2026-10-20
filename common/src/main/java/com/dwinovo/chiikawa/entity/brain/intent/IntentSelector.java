package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Decision;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Scored;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
 * then looks at the generic intents plus those of the pet's capability. A pet goes on with
 * what it is doing unless an intent of a strictly higher priority can start; otherwise it
 * starts the best of the highest priority there is. How much each intent matters is data,
 * see {@link ErrandSettings}.
 */
public final class IntentSelector {
    public static final int EVAL_INTERVAL = 10;
    /** Added to the base score of an intent whose work the pet's slip counts. */
    static final float TASK_BONUS = 0.2F;
    /**
     * Added to work while a pet is in the mood after a proper meal. Enough to win against
     * pottering about, not enough to overrule the owner's directive or a slip it carries —
     * a meal makes a pet keener, not a different pet.
     */
    static final float EAGER_BONUS = 0.15F;

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
        Choice choice = choose(evaluation.candidates(), current, evaluation.context().personality().randomness(),
            pet.getRandom());
        if (choice.keeps(current)) {
            return;
        }
        PetIntent previous = current == null ? null : PetIntents.get(current.id());
        PetIntent next = choice.next() == null ? null : PetIntents.get(choice.next());
        String cause = choice.ended() != null ? choice.ended().name() : trigger.name();
        switchIntent(pet, level, previous, next, now, cause, choice.ranking(), evaluation.context());
    }

    /**
     * Snapshot of every candidate for the debug command. Chooses nothing and draws nothing from the
     * pet's random: looking changes nothing.
     */
    public static Snapshot describe(AbstractPet pet) {
        Evaluation evaluation = evaluate(pet);
        List<CandidateView> views = new ArrayList<>();
        for (int i = 0; i < evaluation.candidates().size(); i++) {
            Candidate candidate = evaluation.candidates().get(i);
            views.add(new CandidateView(evaluation.intents().get(i), candidate.priority(), candidate.allowed(),
                candidate.check(), candidate.score()));
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
     *   <li>A running intent that has not ended keeps running unless an intent of a strictly
     *       higher priority can start: an equal priority never interrupts, however much better
     *       it scores.</li>
     *   <li>Otherwise only the intents of the highest priority that has one that can start are
     *       ranked, by score plus noise of up to {@code randomness}, and the best one runs.</li>
     * </ol>
     *
     * <p>So the score and the noise only decide between intents of one priority, and neither makes
     * a pet drop what it is doing nor lets an intent of a lower priority win.
     *
     * @param candidates offered intents in a stable order, scores already weighted by
     *                   personality; for the running intent the check is its continue
     *                   condition
     * @param current the running intent, or {@code null}
     * @param randomness the pet's personality randomness
     * @param random noise source, see {@link Decision#pickWithinTier}
     * @return the choice
     */
    static Choice choose(List<Candidate> candidates, @Nullable RunningIntent current, float randomness, RandomSource random) {
        Candidate held = null;
        EndReason ended = null;
        if (current != null) {
            held = candidates.stream().filter(c -> c.id().equals(current.id())).findFirst().orElse(null);
            ended = endReason(held);
        }
        boolean keeping = held != null && ended == null;
        List<Scored> startable = candidates.stream().filter(Candidate::eligible)
            .map(c -> new Scored(c.id(), c.priority(), c.score(), List.of(1))).toList();
        Optional<Decision.Pick> pick = Decision.decide(startable, keeping ? held.priority() : Integer.MIN_VALUE, randomness, random);
        if (pick.isEmpty()) {
            return new Choice(keeping ? held.id() : null, ended, List.of());
        }
        int priority = startable.stream().filter(scored -> scored.errand().equals(pick.get().errand()))
            .findFirst().orElseThrow().priority();
        // Stable sort: on equal scores the earlier candidate comes first.
        List<Scored> ranking = startable.stream().filter(scored -> scored.priority() == priority)
            .sorted(Comparator.comparingDouble((Scored scored) -> scored.score()).reversed()).toList();
        return new Choice(pick.get().errand(), ended, ranking);
    }

    /**
     * Candidates as the selector scores them: whether the directive permits each intent,
     * its start condition (continue condition for the running one), its priority from its settings
     * and its base score, raised by {@link #TASK_BONUS} when the pet's slip counts the intent's work,
     * weighted by the personality for the current part of the day. An intent without settings is not
     * permitted. One that yields to another is held back while that one can start.
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
            Optional<ErrandSettings<?>> settings = ErrandSettings.of(intent.id());
            if (settings.isEmpty()) {
                candidates.add(new Candidate(intent.id(), 0, false, IntentCheck.OK, 0.0F));
                continue;
            }
            boolean starting = !intent.id().equals(running);
            candidates.add(new Candidate(
                intent.id(),
                settings.get().priority(),
                allows.test(intent.category()),
                admitted(intent, ctx, starting),
                (intent.score(ctx, settings.get())
                    + (carriesSlipFor(intent, ctx) ? TASK_BONUS : 0.0F)
                    + (eagerFor(intent, ctx) ? EAGER_BONUS : 0.0F))
                    * ctx.personality().multiplier(intent.id(), ctx.phase())
            ));
        }
        Set<ResourceLocation> startable = candidates.stream().filter(Candidate::eligible).map(Candidate::id)
            .collect(Collectors.toSet());
        candidates.replaceAll(candidate -> candidate.eligible() && yieldsToAnyOf(candidate.id(), startable)
            ? candidate.heldBack()
            : candidate);
        return candidates;
    }

    /**
     * Whether an intent yields to one of those that can start: the farmer's strict order, which no
     * score a personality could close. What those intents in turn yield to does not matter, or plant
     * would start the moment harvest was held back by something else.
     */
    private static boolean yieldsToAnyOf(ResourceLocation id, Set<ResourceLocation> startable) {
        return ErrandSettings.of(id).map(settings -> settings.yieldsTo().stream().anyMatch(startable::contains))
            .orElse(false);
    }

    /**
     * The intent's own condition and then the one rule for where it may go: an errand with a
     * destination starts only within the anchor's reach and goes on only within its leash, whatever
     * the errand is. The only place that is checked.
     */
    private static IntentCheck admitted(PetIntent intent, IntentContext ctx, boolean starting) {
        IntentCheck check = starting ? intent.canRun(ctx) : intent.canContinue(ctx);
        if (!check.ok()) {
            return check;
        }
        return intent.destination(ctx)
            .filter(destination -> !ctx.anchor().admits(destination, starting))
            .map(destination -> IntentCheck.fail(starting ? "out_of_reach" : "out_of_leash"))
            .orElse(IntentCheck.OK);
    }

    /** Whether this is work, and the pet has just been fed something worth working off. */
    private static boolean eagerFor(PetIntent intent, IntentContext ctx) {
        return ctx.eager()
            && (intent.category() == IntentCategory.WORK || intent.category() == IntentCategory.FORAGE);
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
            @Nullable PetIntent next, long now, String cause, List<Scored> ranking, IntentContext ctx) {
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
        if (next != null) {
            next.onStart(new IntentRuntime(pet, level, brain), ctx);
        }
        brain.getMemory(InitMemory.INTENT_SWITCH_LOG.get()).ifPresent(log -> log.record(
            now, previous == null ? null : previous.id(), next == null ? null : next.id(), cause, ranking));
        // Setting off to fight, rather than turning from one way of fighting to another, is
        // when a pet shouts its battle cry.
        if (next != null && next.category() == IntentCategory.COMBAT
                && (previous == null || previous.category() != IntentCategory.COMBAT)) {
            PetSpeech.say(pet, VoiceMoment.HUNT);
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
        CONDITION_FAILED
    }

    /**
     * @param priority the priority in the intent's settings
     * @param allowed whether the directive permits the intent's category
     * @param check start condition, or continue condition for the running intent
     * @param score base score weighted by personality
     */
    record Candidate(ResourceLocation id, int priority, boolean allowed, IntentCheck check, float score) {
        boolean eligible() {
            return allowed && check.ok();
        }

        /** @return this candidate, held back by an intent it yields to */
        Candidate heldBack() {
            return new Candidate(id, priority, allowed, IntentCheck.fail("higher_priority"), score);
        }
    }

    /**
     * @param next the intent to run, {@code null} if none can
     * @param ended why the running intent ended, {@code null} if it did not
     * @param ranking the intents of the priority that was chosen from, with their weighted scores, best
     *                first; empty when the running intent is kept
     */
    record Choice(@Nullable ResourceLocation next, @Nullable EndReason ended, List<Scored> ranking) {
        boolean keeps(@Nullable RunningIntent current) {
            return ended == null && Objects.equals(current == null ? null : current.id(), next);
        }
    }

    private record Evaluation(IntentContext context, List<PetIntent> intents, List<Candidate> candidates) {
    }

    /**
     * @param score base score weighted by personality
     */
    public record CandidateView(PetIntent intent, int priority, boolean allowed, IntentCheck check, float score) {
    }

    public record Snapshot(Optional<RunningIntent> running, long gameTime, DayPhase phase, List<CandidateView> candidates) {
    }
}
