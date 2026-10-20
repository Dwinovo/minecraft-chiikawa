package com.dwinovo.chiikawa.entity.brain.errand.decide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.decide.Decision.Pick;
import com.dwinovo.chiikawa.testing.FixedRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** The rules a pet picks its next errand by: priority first, then score and noise, then the offer. */
class DecisionCoreTest {
    private static final ResourceLocation WORK = id("work");
    private static final ResourceLocation PLANT = id("plant");
    private static final ResourceLocation FIGHT = id("fight");
    private static final ResourceLocation PLAY = id("play");
    private static final ResourceLocation WANDER = id("wander");
    /** Nothing may be drawn: a random with nothing in it throws if anything is. */
    private static final RandomSource NO_DRAWS = FixedRandom.floats();

    @Test
    void aHigherPriorityWinsWhateverTheScores() {
        List<Scored> candidates = List.of(scored(WORK, 50, 0.99F), scored(FIGHT, 80, 0.1F), scored(WANDER, 10, 1.0F));

        assertEquals(new Pick(FIGHT, 0), Decision.decide(candidates, Integer.MIN_VALUE, 0.0F, NO_DRAWS).orElseThrow());
    }

    @Test
    void noiseNeverLiftsALowerPriorityOverAHigherOne() {
        List<Scored> candidates = List.of(scored(WORK, 50, 1.0F), scored(FIGHT, 80, 0.0F));

        // Even with all the noise there is going in work's favour, and none drawn for the fight.
        assertEquals(FIGHT, Decision.decide(candidates, Integer.MIN_VALUE, 5.0F, FixedRandom.floats(1.0F)).orElseThrow().errand());
    }

    @Test
    void theFloorExcludesItsOwnPriorityAndEverythingBelow() {
        List<Scored> candidates = List.of(scored(FIGHT, 80, 0.1F), scored(WORK, 50, 0.9F), scored(PLAY, 50, 0.8F),
            scored(WANDER, 10, 1.0F));

        assertEquals(FIGHT, Decision.decide(candidates, 50, 0.0F, NO_DRAWS).orElseThrow().errand());
        assertEquals(Optional.empty(), Decision.decide(candidates, 80, 0.0F, NO_DRAWS));
        assertEquals(WORK, Decision.decide(candidates.subList(1, 4), 10, 0.0F, NO_DRAWS).orElseThrow().errand());
        assertEquals(Optional.empty(), Decision.decide(List.of(scored(WORK, 50, 0.9F)), 50, 0.0F, NO_DRAWS),
            "an errand of the priority the pet is on never interrupts it");
    }

    @Test
    void nothingIsPickedFromNothing() {
        assertEquals(Optional.empty(), Decision.decide(List.of(), Integer.MIN_VALUE, 0.0F, NO_DRAWS));
    }

    @Test
    void theWalkStopsAtTheFirstPriorityWithACandidateAndAsksNoFurther() {
        List<Integer> asked = new ArrayList<>();

        Optional<Pick> pick = Decision.decide(List.of(10, 80, 50, 50, 30), 0, priority -> {
            asked.add(priority);
            return priority == 50 ? List.of(scored(WORK, 50, 0.6F)) : List.of();
        }, 0.0F, NO_DRAWS);

        assertEquals(WORK, pick.orElseThrow().errand());
        assertEquals(List.of(80, 50), asked, "top down, each priority once, and nothing below the one that has a candidate");
    }

    @Test
    void onlyThePrioritiesAboveTheFloorAreAskedAbout() {
        List<Integer> asked = new ArrayList<>();

        Optional<Pick> pick = Decision.decide(List.of(80, 50, 30, 10), 30, priority -> {
            asked.add(priority);
            return List.of();
        }, 0.0F, NO_DRAWS);

        assertEquals(Optional.empty(), pick);
        assertEquals(List.of(80, 50), asked);
    }

    @Test
    void withinAPriorityTheBestScoreWinsAndAnEarlierOneWinsATie() {
        List<Scored> tier = List.of(scored(WORK, 50, 0.6F), scored(PLANT, 50, 0.7F), scored(PLAY, 50, 0.7F));

        assertEquals(PLANT, Decision.pickWithinTier(tier, 0.0F, NO_DRAWS).errand());
    }

    @Test
    void noiseOnlyDecidesBetweenScoresCloserThanTheRandomness() {
        List<Scored> tier = List.of(scored(WORK, 50, 0.6F), scored(PLANT, 50, 0.5F));

        // Work draws the worst noise and plant the best: 0.1 of randomness swings 0.1 either way,
        // and 0.5 + 0.1 is more than 0.6 - 0.1.
        assertEquals(PLANT, Decision.pickWithinTier(tier, 0.1F, FixedRandom.floats(0.0F, 1.0F)).errand());
        // A gap wider than the swing can not be closed.
        assertEquals(WORK, Decision.pickWithinTier(tier, 0.04F, FixedRandom.floats(0.0F, 1.0F)).errand());
        // And without randomness there is no noise to draw.
        assertEquals(WORK, Decision.pickWithinTier(tier, 0.0F, NO_DRAWS).errand());
    }

    @Test
    void aLoneCandidateWithOneOfferDrawsNothing() {
        assertEquals(new Pick(WORK, 0), Decision.pickWithinTier(List.of(scored(WORK, 50, 0.6F)), 0.3F, NO_DRAWS));
    }

    @Test
    void theOfferIsDrawnInProportionToItsWeight() {
        List<Scored> tier = List.of(new Scored(WANDER, 10, 0.05F, List.of(2, 0, 3)));

        // Five tickets in all: 0 and 1 are the first offer's and 2 to 4 the third's; the second has none.
        assertEquals(0, Decision.pickWithinTier(tier, 0.0F, FixedRandom.ints(0)).offer());
        assertEquals(0, Decision.pickWithinTier(tier, 0.0F, FixedRandom.ints(1)).offer());
        assertEquals(2, Decision.pickWithinTier(tier, 0.0F, FixedRandom.ints(2)).offer());
        assertEquals(2, Decision.pickWithinTier(tier, 0.0F, FixedRandom.ints(4)).offer());
    }

    @Test
    void theDrawIsAmongTheOffersOfTheErrandThatWon() {
        List<Scored> tier = List.of(new Scored(WORK, 50, 0.6F, List.of(1)), new Scored(PLANT, 50, 0.9F, List.of(1, 1)));

        assertEquals(new Pick(PLANT, 1), Decision.pickWithinTier(tier, 0.0F, FixedRandom.ints(1)));
    }

    @Test
    void aSeededRandomPicksTheSameEveryTime() {
        List<Scored> tier = List.of(new Scored(WORK, 50, 0.6F, List.of(1, 2, 3)), new Scored(PLANT, 50, 0.58F, List.of(4, 1)));

        for (long seed = 0; seed < 50; seed++) {
            assertEquals(Decision.pickWithinTier(tier, 0.1F, RandomSource.create(seed)),
                Decision.pickWithinTier(tier, 0.1F, RandomSource.create(seed)), "seed " + seed);
        }
    }

    @Test
    void overManyDrawsEveryOfferAndANearTieAreBothPicked() {
        List<Scored> tier = List.of(new Scored(WORK, 50, 0.60F, List.of(1, 1)), new Scored(PLANT, 50, 0.58F, List.of(1)));
        RandomSource random = RandomSource.create(42);
        int[] errands = new int[2];
        int[] offers = new int[2];

        for (int i = 0; i < 2000; i++) {
            Pick pick = Decision.pickWithinTier(tier, 0.1F, random);
            errands[pick.errand().equals(WORK) ? 0 : 1]++;
            if (pick.errand().equals(WORK)) {
                offers[pick.offer()]++;
            }
        }

        assertTrue(errands[0] > errands[1] && errands[1] > 200, "work is likelier, plant still happens: " + errands[0] + "/" + errands[1]);
        assertTrue(offers[0] > 300 && offers[1] > 300, "both offers: " + offers[0] + "/" + offers[1]);
    }

    @Test
    void aCandidateNeedsAnOfferWithAWeight() {
        assertThrows(IllegalArgumentException.class, () -> new Scored(WORK, 50, 0.6F, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Scored(WORK, 50, 0.6F, List.of(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Scored(WORK, 50, 0.6F, List.of(3, -1)));
    }

    @Test
    void thereIsNothingToPickInAnEmptyTier() {
        assertThrows(IllegalArgumentException.class, () -> Decision.pickWithinTier(List.of(), 0.0F, NO_DRAWS));
    }

    private static Scored scored(ResourceLocation errand, int priority, float score) {
        return new Scored(errand, priority, score, List.of(1));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chiikawa", path);
    }
}
