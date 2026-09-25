package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Candidate;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Decision;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.EndReason;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.SelectorParams;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.dwinovo.chiikawa.testing.TestContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class IntentSelectorTest {
    private static final Identifier WANDER = id("wander");
    private static final Identifier HARVEST = id("harvest");
    private static final Identifier PLANT = id("plant");
    private static final Identifier FIGHT = id("fight");
    private static final long NOW = 1000L;
    /** No noise and no hold margin unless a test asks for them. */
    private static final SelectorParams PLAIN = new SelectorParams(0.0F, 0.0F);

    // ---- filtering and scoring -------------------------------------------------

    @Test
    void picksTheHighestScoringEligibleCandidate() {
        Decision decision = choose(List.of(ok(WANDER, 0.05F), ok(PLANT, 0.55F), ok(HARVEST, 0.6F)), null, PLAIN);

        assertEquals(HARVEST, decision.next());
        assertNull(decision.ended());
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
    void noiseOnlyRanksCandidatesWithinTheToleranceOfTheBest() {
        // Tolerance 0.12: plant and harvest are acceptable, wandering is not.
        // nextFloat() == 1.0 gives the full +randomness, 0.0 the full -randomness.
        SelectorParams random = new SelectorParams(0.02F, 0.1F);
        List<Candidate> candidates = List.of(ok(WANDER, 0.05F), ok(PLANT, 0.55F), ok(HARVEST, 0.6F));

        Decision plantLucky = choose(candidates, null, random, FixedRandom.floats(1.0F, 0.0F));
        assertEquals(PLANT, plantLucky.next());
        assertEquals(List.of(PLANT, HARVEST), plantLucky.ranking().stream().map(IntentSelector.Scored::id).toList());
        assertEquals(0.65F, plantLucky.ranking().get(0).score(), 1.0E-6F);
        assertEquals(0.5F, plantLucky.ranking().get(1).score(), 1.0E-6F);

        assertEquals(HARVEST, choose(candidates, null, random, FixedRandom.floats(0.0F, 1.0F)).next());
    }

    // ---- keeping the running intent --------------------------------------------

    @Test
    void keepsTheRunningIntentUntilAnotherBeatsItByMoreThanTheTolerance() {
        SelectorParams random = new SelectorParams(0.02F, 0.1F);
        RunningIntent plant = new RunningIntent(PLANT, NOW - 100);

        assertTrue(choose(List.of(ok(PLANT, 0.55F), ok(HARVEST, 0.66F)), plant, random).keeps(plant));
        assertEquals(HARVEST, choose(List.of(ok(PLANT, 0.55F), ok(HARVEST, 0.68F)), plant, random).next());
    }

    @Test
    void aClearlyMoreImportantIntentTakesOverAtOnceWhateverTheNoise() {
        SelectorParams veryRandom = new SelectorParams(0.02F, 0.3F);
        RunningIntent wander = new RunningIntent(WANDER, NOW - 1);

        // Wandering draws the most positive noise, the fight the most negative.
        Decision decision = choose(List.of(ok(WANDER, 0.05F), ok(FIGHT, 0.8F)), wander, veryRandom, FixedRandom.floats(1.0F, 0.0F));

        assertEquals(FIGHT, decision.next());
    }

    @Test
    void aRandomPetDoesNotFlipBetweenEquallyGoodIntents() {
        SelectorParams veryRandom = new SelectorParams(0.02F, 0.3F);
        List<Candidate> equal = List.of(ok(WANDER, 0.5F), ok(PLANT, 0.5F));
        RunningIntent current = null;
        int switches = 0;
        for (int evaluation = 0; evaluation < 50; evaluation++) {
            // Alternate which one the noise favours on every evaluation.
            RandomSource random = evaluation % 2 == 0 ? FixedRandom.floats(1.0F, 0.0F) : FixedRandom.floats(0.0F, 1.0F);
            Decision decision = choose(equal, current, veryRandom, random);
            if (!decision.keeps(current)) {
                current = new RunningIntent(decision.next(), NOW + evaluation);
                switches++;
            }
        }

        assertEquals(1, switches);
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
    void endsTheRunningIntentWhenTheDirectiveNoLongerPermitsIt() {
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

    // ---- personality -----------------------------------------------------------

    @Test
    void scoresAreWeightedByPersonalityAndTimeOfDay() {
        Personality personality = new Personality(Map.of(PetIntents.WANDER, 2.0F),
            Map.of(DayPhase.NIGHT, Map.of(PetIntents.WANDER, 3.0F)), 0.0F, List.of(), List.of(), IdleHabits.DEFAULT);

        List<Candidate> day = farmCandidates(personality, DayPhase.DAY, targets(false, false, false), null);
        List<Candidate> night = farmCandidates(personality, DayPhase.NIGHT, targets(false, false, false), null);

        assertEquals(0.1F, day.get(0).score(), 1.0E-6F);
        assertEquals(0.3F, night.get(0).score(), 1.0E-6F);
    }

    @Test
    void farmerOrderHoldsWhateverThePersonality() {
        // Multipliers that would put deliver over plant over harvest, and as much noise as a
        // personality may have.
        Personality inverted = new Personality(
            Map.of(PetIntents.HARVEST, 0.1F, PetIntents.PLANT, 5.0F, PetIntents.DELIVER, 10.0F),
            Map.of(DayPhase.DAY, Map.of(PetIntents.PLANT, 2.0F)), 1.0F, List.of(), List.of(), IdleHabits.DEFAULT);
        SelectorParams params = new SelectorParams(IntentSelector.HOLD_MARGIN, inverted.randomness());

        for (float noise = 0.0F; noise <= 1.0F; noise += 0.125F) {
            Decision everything = IntentSelector.choose(
                farmCandidates(inverted, DayPhase.DAY, targets(true, true, true), null), null, params,
                FixedRandom.floats(noise, 1.0F - noise));
            assertTrue(Set.of(PetIntents.WANDER, PetIntents.HARVEST).contains(everything.next()), everything::toString);
        }

        RunningIntent planting = new RunningIntent(PetIntents.PLANT, NOW - 5);
        Decision cropRipened = IntentSelector.choose(
            farmCandidates(inverted, DayPhase.DAY, targets(true, true, true), PetIntents.PLANT), planting, params,
            FixedRandom.floats(0.5F));
        assertEquals(EndReason.CONDITION_FAILED, cropRipened.ended());

        List<Candidate> plantAndDeliver = farmCandidates(inverted, DayPhase.DAY, targets(false, true, true), null);
        assertTrue(plantAndDeliver.get(2).eligible());
        assertEquals("intent.chiikawa.fail.higher_priority", plantAndDeliver.get(3).check().reasonKey());
    }

    // ---- slips -----------------------------------------------------------------

    @Test
    void workTheCarriedSlipCountsIsPreferred() {
        PetTask weeding = new PetTask(id("weeding"), id("farmer"), PetWorkCounters.WEED, PetTask.NO_ICON, 8,
            ResourceKey.create(Registries.LOOT_TABLE, id("pet_task/weeding")), 0);
        Optional<GlobalPos> near = near(true, 1);
        IntentContext ctx = TestContext.at(PET, new PetAnchor(PET, AnchorDistances.FREE_REACH, AnchorDistances.FREE_LEASH, false, true))
            .targets(new PerceivedTargets(Optional.empty(), near, Optional.empty(), Optional.empty(), near, Optional.empty(),
                Optional.empty()))
            .task(weeding)
            .build();
        List<Candidate> candidates = IntentSelector.candidates(
            List.of(PetIntents.get(PetIntents.HARVEST), PetIntents.get(PetIntents.WEED)), ctx, null, category -> true);

        assertEquals(0.6F, candidates.get(0).score(), 1.0E-6F);
        assertEquals(0.5F + IntentSelector.TASK_BONUS, candidates.get(1).score(), 1.0E-6F);
        assertEquals(PetIntents.WEED, choose(candidates, null, PLAIN).next());
    }

    // ---- a good meal -----------------------------------------------------------

    @Test
    void aMealTipsTheScalesTowardsWorkAndNothingElse() {
        PerceivedTargets crop = targets(true, false, false);
        List<PetIntent> offered = List.of(PetIntents.get(PetIntents.WANDER), PetIntents.get(PetIntents.HARVEST));
        List<Candidate> hungry = IntentSelector.candidates(offered, fed(crop, false), null, category -> true);
        List<Candidate> full = IntentSelector.candidates(offered, fed(crop, true), null, category -> true);

        assertEquals(hungry.get(0).score(), full.get(0).score(), 1.0E-6F);
        assertEquals(hungry.get(1).score() + IntentSelector.EAGER_BONUS, full.get(1).score(), 1.0E-6F);
    }

    @Test
    void aMealIsNotWorthAsMuchAsTheSlipThePetCarries() {
        assertTrue(IntentSelector.EAGER_BONUS < IntentSelector.TASK_BONUS);
    }

    // ---- helpers ---------------------------------------------------------------

    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
        Identifier.withDefaultNamespace("overworld"));
    private static final GlobalPos PET = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));

    /** A free pet with a crop in sight, either freshly fed or not. */
    private static IntentContext fed(PerceivedTargets targets, boolean eager) {
        TestContext ctx = TestContext.at(PET,
                new PetAnchor(PET, AnchorDistances.FREE_REACH, AnchorDistances.FREE_LEASH, false, true))
            .targets(targets);
        return (eager ? ctx.eager() : ctx).build();
    }

    /** Wander, harvest, plant and deliver, as a free farmer sees them. */
    private static List<Candidate> farmCandidates(Personality personality, DayPhase phase, PerceivedTargets targets,
            Identifier running) {
        IntentContext ctx = TestContext.at(PET, new PetAnchor(PET, AnchorDistances.FREE_REACH, AnchorDistances.FREE_LEASH, false, true))
            .phase(phase)
            .personality(personality)
            .targets(targets)
            .build();
        List<PetIntent> offered = List.of(PetIntents.get(PetIntents.WANDER), PetIntents.get(PetIntents.HARVEST),
            PetIntents.get(PetIntents.PLANT), PetIntents.get(PetIntents.DELIVER));
        return IntentSelector.candidates(offered, ctx, running, category -> true);
    }

    private static PerceivedTargets targets(boolean crop, boolean farmland, boolean container) {
        return new PerceivedTargets(Optional.empty(), near(crop, 1), near(farmland, 2), near(container, 3),
            Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static Optional<GlobalPos> near(boolean present, int blocksEast) {
        return present ? Optional.of(GlobalPos.of(OVERWORLD, PET.pos().east(blocksEast))) : Optional.empty();
    }

    private static Decision choose(List<Candidate> candidates, RunningIntent current, SelectorParams params) {
        return choose(candidates, current, params, FixedRandom.floats(0.5F));
    }

    private static Decision choose(List<Candidate> candidates, RunningIntent current, SelectorParams params, RandomSource random) {
        return IntentSelector.choose(candidates, current, params, random);
    }

    private static Candidate ok(Identifier id, float score) {
        return new Candidate(id, true, IntentCheck.OK, score);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("chiikawa", path);
    }
}
