package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Candidate;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Decision;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.EndReason;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.SelectorParams;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.junit.jupiter.api.Test;

class IntentSelectorTest {
    private static final ResourceLocation WANDER = id("wander");
    private static final ResourceLocation HARVEST = id("harvest");
    private static final ResourceLocation PLANT = id("plant");
    private static final ResourceLocation FIGHT = id("fight");
    private static final long NOW = 1000L;
    /** No noise and no hold bonus unless a test asks for them. */
    private static final SelectorParams PLAIN = new SelectorParams(NOW, 0.0F, 0.0F, 40);

    // ---- filtering and scoring -------------------------------------------------

    @Test
    void picksTheHighestScoringEligibleCandidate() {
        Decision decision = choose(List.of(ok(WANDER, 0.05F), ok(PLANT, 0.55F), ok(HARVEST, 0.6F)), null, PLAIN);

        assertEquals(HARVEST, decision.next());
        assertNull(decision.ended());
        assertEquals(List.of(HARVEST, PLANT, WANDER), decision.ranking().stream().map(IntentSelector.Scored::id).toList());
    }

    @Test
    void skipsCandidatesTheDirectiveForbidsOrWhoseConditionFails() {
        Decision decision = choose(List.of(
            ok(WANDER, 0.05F),
            new Candidate(FIGHT, false, IntentCheck.OK, 0.8F),
            new Candidate(HARVEST, true, IntentCheck.fail("no_crop"), 0.6F)
        ), null, PLAIN);

        assertEquals(WANDER, decision.next());
        assertEquals(1, decision.ranking().size());
    }

    @Test
    void returnsNothingWhenNoCandidateCanRun() {
        Decision decision = choose(List.of(new Candidate(FIGHT, false, IntentCheck.OK, 0.8F)), null, PLAIN);

        assertNull(decision.next());
        assertTrue(decision.keeps(null));
    }

    @Test
    void addsInjectedNoiseToEachScoredCandidate() {
        // nextFloat() == 1.0 gives the full +jitter, 0.0 the full -jitter.
        SelectorParams noisy = new SelectorParams(NOW, 0.0F, 0.1F, 40);
        List<Candidate> close = List.of(ok(PLANT, 0.55F), ok(HARVEST, 0.6F));

        Decision raised = choose(close, null, noisy, new FixedRandom(1.0F));
        assertEquals(HARVEST, raised.next());
        assertEquals(0.7F, raised.ranking().get(0).score(), 1.0E-6F);

        Decision alternating = choose(close, null, noisy, new FixedRandom(1.0F, 0.0F));
        assertEquals(PLANT, alternating.next());
        assertEquals(0.65F, alternating.ranking().get(0).score(), 1.0E-6F);
        assertEquals(0.5F, alternating.ranking().get(1).score(), 1.0E-6F);
    }

    // ---- keeping the running intent --------------------------------------------

    @Test
    void holdBonusKeepsTheRunningIntentAgainstASlightlyBetterOne() {
        SelectorParams hold = new SelectorParams(NOW, 0.1F, 0.0F, 40);
        RunningIntent plant = new RunningIntent(PLANT, NOW - 100);

        assertTrue(choose(List.of(ok(PLANT, 0.55F), ok(HARVEST, 0.6F)), plant, hold).keeps(plant));
        assertEquals(HARVEST, choose(List.of(ok(PLANT, 0.55F), ok(HARVEST, 0.7F)), plant, hold).next());
    }

    @Test
    void minimumDwellOnlyLetsAHigherBaseScoreTakeOver() {
        SelectorParams noisy = new SelectorParams(NOW, 0.0F, 0.1F, 40);
        List<Candidate> sameBase = List.of(ok(WANDER, 0.5F), ok(PLANT, 0.5F));

        // The running intent draws the most negative noise, the challenger the most positive.
        RunningIntent fresh = new RunningIntent(WANDER, NOW - 39);
        assertTrue(choose(sameBase, fresh, noisy, new FixedRandom(0.0F, 1.0F)).keeps(fresh));

        RunningIntent settled = new RunningIntent(WANDER, NOW - 40);
        assertEquals(PLANT, choose(sameBase, settled, noisy, new FixedRandom(0.0F, 1.0F)).next());

        assertEquals(HARVEST, choose(List.of(ok(WANDER, 0.5F), ok(HARVEST, 0.6F)), fresh, noisy, new FixedRandom(0.5F)).next());
    }

    // ---- ending the running intent ---------------------------------------------

    @Test
    void endsTheRunningIntentWhenItsConditionFails() {
        RunningIntent harvest = new RunningIntent(HARVEST, NOW - 5);
        Decision decision = choose(List.of(ok(WANDER, 0.05F),
            new Candidate(HARVEST, true, IntentCheck.fail("no_crop"), 0.6F)), harvest, PLAIN);

        assertEquals(EndReason.CONDITION_FAILED, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void endsTheRunningIntentWhenTheDirectiveNoLongerPermitsItEvenDuringDwell() {
        RunningIntent fight = new RunningIntent(FIGHT, NOW - 5);
        Decision decision = choose(List.of(ok(WANDER, 0.05F),
            new Candidate(FIGHT, false, IntentCheck.OK, 0.8F)), fight, PLAIN);

        assertEquals(EndReason.NOT_ALLOWED, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void endsTheRunningIntentWhenItIsNoLongerOffered() {
        RunningIntent fight = new RunningIntent(FIGHT, NOW - 5);
        Decision decision = choose(List.of(ok(WANDER, 0.05F)), fight, PLAIN);

        assertEquals(EndReason.NOT_OFFERED, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void anEndedIntentDoesNotCompeteInTheSameEvaluation() {
        RunningIntent harvest = new RunningIntent(HARVEST, NOW - 5);
        Decision decision = choose(List.of(new Candidate(HARVEST, false, IntentCheck.OK, 0.6F)), harvest, PLAIN);

        assertEquals(EndReason.NOT_ALLOWED, decision.ended());
        assertNull(decision.next());
    }

    // ---- helpers ---------------------------------------------------------------

    private static Decision choose(List<Candidate> candidates, RunningIntent current, SelectorParams params) {
        return choose(candidates, current, params, new FixedRandom(0.5F));
    }

    private static Decision choose(List<Candidate> candidates, RunningIntent current, SelectorParams params, RandomSource random) {
        return IntentSelector.choose(candidates, current, params, random);
    }

    private static Candidate ok(ResourceLocation id, float score) {
        return new Candidate(id, true, IntentCheck.OK, score);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chiikawa", path);
    }

    /** Returns the given floats in turn, repeating the last one. */
    private static final class FixedRandom implements RandomSource {
        private final float[] values;
        private int next;

        FixedRandom(float... values) {
            this.values = values;
        }

        @Override
        public float nextFloat() {
            float value = values[Math.min(next, values.length - 1)];
            next++;
            return value;
        }

        @Override
        public RandomSource fork() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSeed(long seed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextInt() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextInt(int bound) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean nextBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double nextDouble() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double nextGaussian() {
            throw new UnsupportedOperationException();
        }
    }
}
