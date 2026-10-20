package com.dwinovo.chiikawa.entity.brain.errand.decide;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

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
    /**
     * @param errand the errand to start
     * @param offer which of its offers to start it with, as an index into its weights
     */
    public record Pick(ResourceLocation errand, int offer) {
    }

    private Decision() {
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
