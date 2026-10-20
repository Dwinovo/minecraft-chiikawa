package com.dwinovo.chiikawa.entity.brain.errand.decide;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * What a pet starts next, from the errands it could start: the highest priority that has one wins
 * outright, whatever the scores; among the errands of that priority the score decides, with the pet's
 * randomness added so near-equal errands take turns; and among the ways to go about the errand
 * chosen, the weights decide.
 *
 * <p>Pure: knows nothing of pets, brains or the world, only of what it is given, so it can be tried
 * with a seeded random. Priority is never traded for score, which is why a pet at work is not taken
 * off it by something of the same priority, and why a personality cannot make play outrank an exam.
 */
public final class Decision {
    /** Added to the base score of an errand whose work the pet's slip counts. */
    public static final float TASK_BONUS = 0.2F;
    /**
     * Added to work while a pet is in the mood after a proper meal. Enough to win against
     * pottering about, not enough to overrule the owner's directive or a slip it carries:
     * a meal makes a pet keener, not a different pet.
     */
    public static final float EAGER_BONUS = 0.15F;

    /**
     * @param errand the errand to start
     * @param offer which of its offers to start it with, as an index into its weights
     */
    public record Pick(ResourceLocation errand, int offer) {
    }

    private Decision() {
    }

    /**
     * What the pet starts: the errand, how it is set about and the settings it starts with.
     *
     * @param type the errand
     * @param settings its settings, which it keeps for as long as it runs
     * @param offer the way chosen to go about it
     * @param ranking the errands of the priority it was chosen from, with the scores they had, best first
     */
    public record Started(ErrandType<?, ?> type, ErrandSettings<?> settings, Offer<?> offer, List<Scored> ranking) {
        /** @return the steps of the errand for this start */
        public Routine routine() {
            return routineOf(type, settings, offer);
        }

        @SuppressWarnings("unchecked")
        private static <P extends Plan, T> Routine routineOf(ErrandType<P, T> type, ErrandSettings<?> settings, Offer<?> offer) {
            return type.routine((P) offer.plan(), (ErrandSettings<T>) settings);
        }
    }

    /**
     * Decides what the pet starts. Walks the priorities from the top down to the floor and stops at the
     * first that has an errand that can start: of the errands offered, only those that get through
     * the filter ({@code Admission}) and are not held back by an errand they yield to compete, and
     * of those the best by score, noise and the draw among its offers deciding as in
     * {@link #pickWithinTier}. What is costly to find out about the pet is found out only for the priorities
     * that are looked at. An errand that yields to another that can start is held back whatever
     * its own score: the farmer's strict order, which no score a personality could close. What those
     * errands in turn yield to does not matter, or plant would start the moment harvest was held back
     * by something else.
     *
     * @param offered the errands the pet's capability offers, in a stable order
     * @param view the pet
     * @param permits what the pet is allowed to do
     * @param rests what the pet is resting from
     * @param floorExclusive the priority of the errand the pet is on, or {@link Integer#MIN_VALUE} for none:
     *                       only a strictly higher priority is looked at, so an equal one never interrupts
     * @param random the noise and the draw among offers; {@code null} to only look: every priority above
     *               the floor is gone through and noted in the trace, and nothing is picked or drawn
     * @param now the current game time
     * @param trace where to note how each errand fared, if somebody is looking
     * @return what to start, or nothing when no priority above the floor has an errand that can
     */
    public static Optional<Started> decide(List<ErrandType<?, ?>> offered, PetView view, Permits permits, RestLedger rests,
            int floorExclusive, @Nullable RandomSource random, long now, @Nullable DecisionTrace trace) {
        Map<ErrandType<?, ?>, ErrandSettings<?>> settings = new LinkedHashMap<>();
        for (ErrandType<?, ?> type : offered) {
            ErrandSettings.of(type.id()).ifPresentOrElse(found -> settings.put(type, found), () -> {
                if (trace != null) {
                    trace.excluded(type.id(), 0, DecisionTrace.SWITCHED_OFF);
                }
            });
        }
        List<Integer> priorities = settings.values().stream().map(ErrandSettings::priority).distinct()
            .sorted(Comparator.reverseOrder()).toList();
        Optional<Started> started = Optional.empty();
        for (int priority : priorities) {
            if (priority <= floorExclusive) {
                break;
            }
            List<Admission<?, ?>> admitted = new ArrayList<>();
            settings.forEach((type, found) -> {
                if (found.priority() == priority) {
                    Admission.of(type, found, view, permits, rests, now, trace).ifPresent(admitted::add);
                }
            });
            List<Admission<?, ?>> tier = holdBack(admitted, trace);
            if (tier.isEmpty()) {
                continue;
            }
            List<Scored> scored = tier.stream().map(admission -> admission.scored(view)).toList();
            if (trace != null) {
                scored.forEach(trace::startable);
            }
            if (random == null) {
                continue;
            }
            Pick pick = pickWithinTier(scored, view.personality().randomness(), random);
            if (trace != null) {
                trace.picked(pick);
            }
            Admission<?, ?> chosen = tier.stream().filter(admission -> admission.type().id().equals(pick.errand()))
                .findFirst().orElseThrow();
            List<Scored> ranking = scored.stream()
                .sorted(Comparator.comparingDouble((Scored candidate) -> candidate.score()).reversed()).toList();
            started = Optional.of(new Started(chosen.type(), chosen.settings(), chosen.offers().get(pick.offer()), ranking));
            break;
        }
        return started;
    }

    /** Leaves out the errands that yield to another of those that can start. */
    private static List<Admission<?, ?>> holdBack(List<Admission<?, ?>> admitted, @Nullable DecisionTrace trace) {
        Set<ResourceLocation> startable = new HashSet<>();
        admitted.forEach(admission -> startable.add(admission.type().id()));
        List<Admission<?, ?>> tier = new ArrayList<>();
        for (Admission<?, ?> admission : admitted) {
            if (admission.settings().yieldsTo().stream().anyMatch(startable::contains)) {
                if (trace != null) {
                    trace.excluded(admission.type().id(), admission.settings().priority(), Check.fail("higher_priority").reasonKey());
                }
            } else {
                tier.add(admission);
            }
        }
        return tier;
    }

    /**
     * Picks among errands that are all known to be startable.
     *
     * @param candidates every errand the pet could start
     * @param floorExclusive the priority of the errand the pet is on, or {@link Integer#MIN_VALUE} for none:
     *                       only a strictly higher priority is looked at, so an equal one never interrupts
     * @param randomness the pet's randomness
     * @param random the noise and the draw among offers
     * @return what to start, or nothing when no candidate is above the floor
     */
    public static Optional<Pick> decide(List<Scored> candidates, int floorExclusive, float randomness, RandomSource random) {
        List<Integer> priorities = candidates.stream().map(Scored::priority).toList();
        return decide(priorities, floorExclusive, priority -> candidates.stream()
            .filter(candidate -> candidate.priority() == priority).toList(), randomness, random);
    }

    /**
     * Walks the priorities from the top down to the floor and stops at the first that has a candidate.
     * The candidates of a priority are asked for only when the walk gets there, so what is costly to
     * find out is found out only for the priorities that are looked at.
     *
     * @param priorities the priorities there are errands at, in any order and with repeats
     * @param floorExclusive the priority of the errand the pet is on, or {@link Integer#MIN_VALUE} for none
     * @param startableAt the errands the pet could start at a priority
     * @param randomness the pet's randomness
     * @param random the noise and the draw among offers
     * @return what to start, or nothing when no priority above the floor has a candidate
     */
    public static Optional<Pick> decide(List<Integer> priorities, int floorExclusive,
            IntFunction<List<Scored>> startableAt, float randomness, RandomSource random) {
        List<Integer> descending = priorities.stream().distinct().sorted(Comparator.reverseOrder()).toList();
        for (int priority : descending) {
            if (priority <= floorExclusive) {
                break;
            }
            List<Scored> tier = startableAt.apply(priority);
            if (!tier.isEmpty()) {
                return Optional.of(pickWithinTier(tier, randomness, random));
            }
        }
        return Optional.empty();
    }

    /**
     * The best of errands of one priority: the score plus noise of up to {@code randomness} either way,
     * highest first, the earlier winning a tie; then a draw among the winner's offers by weight.
     * Draws nothing from the random where nothing is undecided: one candidate, no randomness, one offer.
     *
     * @param tier the errands, all of one priority, in a stable order
     * @param randomness the pet's randomness
     * @param random one draw for each candidate in order if there is more than one and randomness is above 0,
     *               then one for the offer if the winner has more than one with a weight
     * @return the winner and the offer
     */
    public static Pick pickWithinTier(List<Scored> tier, float randomness, RandomSource random) {
        if (tier.isEmpty()) {
            throw new IllegalArgumentException("No errand to pick from");
        }
        Scored winner = tier.get(0);
        if (tier.size() > 1) {
            float best = Float.NEGATIVE_INFINITY;
            for (Scored candidate : tier) {
                float noise = randomness > 0.0F ? randomness * (random.nextFloat() * 2.0F - 1.0F) : 0.0F;
                if (candidate.score() + noise > best) {
                    best = candidate.score() + noise;
                    winner = candidate;
                }
            }
        }
        return new Pick(winner.errand(), drawOffer(winner.offerWeights(), random));
    }

    private static int drawOffer(List<Integer> weights, RandomSource random) {
        if (weights.size() == 1) {
            return 0;
        }
        int roll = random.nextInt(weights.stream().mapToInt(Integer::intValue).sum());
        for (int offer = 0; offer < weights.size(); offer++) {
            roll -= weights.get(offer);
            if (roll < 0) {
                return offer;
            }
        }
        throw new IllegalStateException("A roll below the total weight left no offer");
    }
}
