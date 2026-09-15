package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Candidate;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Decision;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.EndReason;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Running;
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
            new Candidate(FIGHT, false, 0L, IntentCheck.OK, 0.8F),
            new Candidate(HARVEST, true, 0L, IntentCheck.fail("no_crop"), 0.6F)
        ), null, PLAIN);

        assertEquals(WANDER, decision.next());
        assertEquals(1, decision.ranking().size());
    }

    @Test
    void skipsCandidatesOnCooldownUntilItEnds() {
        List<Candidate> candidates = List.of(ok(WANDER, 0.05F), new Candidate(HARVEST, true, NOW + 1, IntentCheck.OK, 0.6F));

        assertEquals(WANDER, choose(candidates, null, PLAIN).next());
        assertEquals(HARVEST, choose(candidates, null, new SelectorParams(NOW + 1, 0.0F, 0.0F, 40)).next());
    }

    @Test
    void returnsNothingWhenNoCandidateCanRun() {
        Decision decision = choose(List.of(new Candidate(FIGHT, false, 0L, IntentCheck.OK, 0.8F)), null, PLAIN);

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
        Running plant = running(PLANT, NOW - 100);

        assertTrue(choose(List.of(ok(PLANT, 0.55F), ok(HARVEST, 0.6F)), plant, hold).keeps(plant));
        assertEquals(HARVEST, choose(List.of(ok(PLANT, 0.55F), ok(HARVEST, 0.7F)), plant, hold).next());
    }

    @Test
    void minimumDwellOnlyLetsAHigherBaseScoreTakeOver() {
        SelectorParams noisy = new SelectorParams(NOW, 0.0F, 0.1F, 40);
        // The running intent draws the most negative noise, the challenger the most positive.
        FixedRandom favoursChallenger = new FixedRandom(0.0F, 1.0F);
        List<Candidate> sameBase = List.of(ok(WANDER, 0.5F), ok(PLANT, 0.5F));

        Running fresh = running(WANDER, NOW - 39);
        assertTrue(choose(sameBase, fresh, noisy, favoursChallenger).keeps(fresh));

        Running settled = running(WANDER, NOW - 40);
        assertEquals(PLANT, choose(sameBase, settled, noisy, new FixedRandom(0.0F, 1.0F)).next());

        assertEquals(HARVEST, choose(List.of(ok(WANDER, 0.5F), ok(HARVEST, 0.6F)), fresh, noisy, new FixedRandom(0.5F)).next());
    }

    // ---- ending the running intent ---------------------------------------------

    @Test
    void endsTheRunningIntentWhenItsConditionFails() {
        Running harvest = running(HARVEST, NOW - 5);
        Decision decision = choose(List.of(ok(WANDER, 0.05F),
            new Candidate(HARVEST, true, 0L, IntentCheck.fail("no_crop"), 0.6F)), harvest, PLAIN);

        assertEquals(EndReason.CONDITION_FAILED, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void endsTheRunningIntentWhenTheDirectiveNoLongerPermitsIt() {
        Running fight = new Running(FIGHT, NOW - 5, new TestStep(false, 0), NOW - 5, false);
        Decision decision = choose(List.of(ok(WANDER, 0.05F),
            new Candidate(FIGHT, false, 0L, IntentCheck.OK, 0.8F)), fight, PLAIN);

        assertEquals(EndReason.NOT_ALLOWED, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void endsTheRunningIntentWhenItIsNoLongerOffered() {
        Running fight = running(FIGHT, NOW - 5);
        Decision decision = choose(List.of(ok(WANDER, 0.05F)), fight, PLAIN);

        assertEquals(EndReason.NOT_OFFERED, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void endsAnIntentWhoseLastStepIsDone() {
        Running done = new Running(HARVEST, NOW - 5, null, NOW - 1, true);
        Decision decision = choose(List.of(ok(WANDER, 0.05F), ok(HARVEST, 0.6F)), done, PLAIN);

        assertEquals(EndReason.COMPLETED, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void uninterruptibleStepIsKeptEvenAgainstABetterCandidate() {
        Running claiming = new Running(PLANT, NOW - 100, new TestStep(false, 20), NOW - 10, false);
        Decision decision = choose(List.of(ok(PLANT, 0.3F), ok(FIGHT, 0.9F)), claiming, PLAIN);

        assertTrue(decision.keeps(claiming));
    }

    @Test
    void timedOutStepEndsTheIntentAndLetsAnotherRun() {
        Running claiming = new Running(PLANT, NOW - 100, new TestStep(false, 20), NOW - 20, false);
        Decision decision = choose(List.of(ok(PLANT, 0.9F), ok(WANDER, 0.05F)), claiming, PLAIN);

        assertEquals(EndReason.STEP_TIMEOUT, decision.ended());
        assertEquals(WANDER, decision.next());
    }

    @Test
    void stepWithoutTimeoutNeverTimesOut() {
        Running walking = new Running(PLANT, NOW - 100000, new TestStep(true, 0), NOW - 100000, false);

        assertTrue(choose(List.of(ok(PLANT, 0.5F), ok(WANDER, 0.05F)), walking, PLAIN).keeps(walking));
    }

    // ---- helpers ---------------------------------------------------------------

    private static Decision choose(List<Candidate> candidates, Running current, SelectorParams params) {
        return choose(candidates, current, params, new FixedRandom(0.5F));
    }

    private static Decision choose(List<Candidate> candidates, Running current, SelectorParams params, RandomSource random) {
        return IntentSelector.choose(candidates, current, params, random);
    }

    private static Candidate ok(ResourceLocation id, float score) {
        return new Candidate(id, true, 0L, IntentCheck.OK, score);
    }

    private static Running running(ResourceLocation id, long startTick) {
        return new Running(id, startTick, null, startTick, false);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("chiikawa", path);
    }

    private record TestStep(boolean interruptible, int timeoutTicks) implements IntentStep {
        @Override
        public String id() {
            return "test";
        }
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
