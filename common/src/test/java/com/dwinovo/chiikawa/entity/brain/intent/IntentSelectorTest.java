package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Scored;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Candidate;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Choice;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.EndReason;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestContext;
import com.dwinovo.chiikawa.testing.TestErrands;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The rules of the selector: a higher priority interrupts a lower one, an equal one never does, the
 * score and the noise only count among intents of one priority, and an intent that yields to another
 * waits for it.
 */
class IntentSelectorTest {
    private static final ResourceLocation WANDER = id("wander");
    private static final ResourceLocation HARVEST = id("harvest");
    private static final ResourceLocation PLANT = id("plant");
    private static final ResourceLocation FIGHT = id("fight");
    private static final long NOW = 1000L;
    private static final int LOW = 10;
    private static final int MIDDLE = 50;
    private static final int HIGH = 80;

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    // ---- filtering and ranking --------------------------------------------------

    @Test
    void picksTheHighestScoringCandidateOfTheHighestPriority() {
        Choice choice = choose(List.of(ok(WANDER, LOW, 0.05F), ok(PLANT, MIDDLE, 0.55F), ok(HARVEST, MIDDLE, 0.6F)), null, 0.0F);

        assertEquals(HARVEST, choice.next());
        assertNull(choice.ended());
        assertEquals(List.of(HARVEST, PLANT), choice.ranking().stream().map(Scored::errand).toList());
    }

    @Test
    void aHigherPriorityWinsWhateverItsScore() {
        Choice choice = choose(List.of(ok(HARVEST, MIDDLE, 0.99F), ok(FIGHT, HIGH, 0.1F)), null, 0.0F);

        assertEquals(FIGHT, choice.next());
        assertEquals(List.of(FIGHT), choice.ranking().stream().map(Scored::errand).toList());
    }

    @Test
    void skipsCandidatesTheDirectiveForbidsOrWhoseConditionFails() {
        Choice choice = choose(List.of(
            ok(WANDER, LOW, 0.05F),
            new Candidate(FIGHT, HIGH, false, IntentCheck.OK, 0.8F),
            new Candidate(HARVEST, MIDDLE, true, IntentCheck.fail("no_crop"), 0.6F)
        ), null, 0.0F);

        assertEquals(WANDER, choice.next());
        assertEquals(1, choice.ranking().size());
    }

    @Test
    void returnsNothingWhenNoCandidateCanRun() {
        Choice choice = choose(List.of(new Candidate(FIGHT, HIGH, false, IntentCheck.OK, 0.8F)), null, 0.0F);

        assertNull(choice.next());
        assertTrue(choice.keeps(null));
    }

    // ---- noise ------------------------------------------------------------------

    @Test
    void noiseOnlyDecidesBetweenCandidatesOfOnePriority() {
        // nextFloat() == 1.0 gives the full +randomness, 0.0 the full -randomness.
        List<Candidate> candidates = List.of(ok(WANDER, LOW, 0.05F), ok(PLANT, MIDDLE, 0.55F), ok(HARVEST, MIDDLE, 0.6F));

        assertEquals(PLANT, choose(candidates, null, 0.1F, FixedRandom.floats(1.0F, 0.0F)).next());
        assertEquals(HARVEST, choose(candidates, null, 0.1F, FixedRandom.floats(0.0F, 1.0F)).next());
    }

    @Test
    void noiseNeverLiftsALowerPriorityOverAHigherOne() {
        // The most noise a pet can have, all of it in favour of wandering.
        List<Candidate> candidates = List.of(ok(WANDER, LOW, 0.9F), ok(HARVEST, MIDDLE, 0.1F));

        assertEquals(HARVEST, choose(candidates, null, 1.0F, FixedRandom.floats(1.0F, 0.0F)).next());
        assertEquals(HARVEST, choose(candidates, null, 1.0F, FixedRandom.floats(0.0F, 1.0F)).next());
    }

    @Test
    void nothingIsDrawnWhereNothingIsUndecided() {
        // FixedRandom throws on a draw it has no value for.
        assertEquals(HARVEST, choose(List.of(ok(WANDER, LOW, 0.05F), ok(HARVEST, MIDDLE, 0.6F)), null, 1.0F, FixedRandom.floats()).next());
        assertEquals(HARVEST, choose(List.of(ok(PLANT, MIDDLE, 0.5F), ok(HARVEST, MIDDLE, 0.6F)), null, 0.0F, FixedRandom.floats()).next());
    }

    // ---- keeping the running intent ---------------------------------------------

    @Test
    void anEqualPriorityNeverInterrupts() {
        RunningIntent plant = new RunningIntent(PLANT, NOW - 100);
        List<Candidate> candidates = List.of(ok(PLANT, MIDDLE, 0.1F), ok(HARVEST, MIDDLE, 0.99F));

        // However far the other scores, and whatever the noise.
        for (float randomness : new float[] {0.0F, 0.3F, 1.0F}) {
            assertTrue(choose(candidates, plant, randomness, FixedRandom.floats(0.0F, 1.0F)).keeps(plant));
            assertTrue(choose(candidates, plant, randomness, FixedRandom.floats(1.0F, 0.0F)).keeps(plant));
        }
    }

    @Test
    void aHigherPriorityInterruptsAtOnceWhateverTheNoise() {
        RunningIntent wander = new RunningIntent(WANDER, NOW - 1);
        // Wandering draws the most positive noise, the fight the most negative.
        Choice choice = choose(List.of(ok(WANDER, LOW, 0.9F), ok(FIGHT, HIGH, 0.1F)), wander, 0.3F, FixedRandom.floats(1.0F, 0.0F));

        assertEquals(FIGHT, choice.next());
        assertNull(choice.ended());
        assertFalse(choice.keeps(wander));
    }

    @Test
    void aLowerPriorityNeverInterrupts() {
        RunningIntent fight = new RunningIntent(FIGHT, NOW - 1);

        assertTrue(choose(List.of(ok(FIGHT, HIGH, 0.1F), ok(WANDER, LOW, 1.0F)), fight, 0.0F).keeps(fight));
    }

    @Test
    void aRandomPetDoesNotFlipBetweenEqualIntents() {
        List<Candidate> equal = List.of(ok(WANDER, MIDDLE, 0.5F), ok(PLANT, MIDDLE, 0.5F));
        RunningIntent current = null;
        int switches = 0;
        for (int evaluation = 0; evaluation < 50; evaluation++) {
            // Alternate which one the noise favours on every evaluation.
            RandomSource random = evaluation % 2 == 0 ? FixedRandom.floats(1.0F, 0.0F) : FixedRandom.floats(0.0F, 1.0F);
            Choice choice = choose(equal, current, 0.3F, random);
            if (!choice.keeps(current)) {
                current = new RunningIntent(choice.next(), NOW + evaluation);
                switches++;
            }
        }

        assertEquals(1, switches);
    }

    // ---- ending the running intent ----------------------------------------------

    @Test
    void endsTheRunningIntentWhenItsConditionFails() {
        RunningIntent harvest = new RunningIntent(HARVEST, NOW - 5);
        Choice choice = choose(List.of(ok(WANDER, LOW, 0.05F),
            new Candidate(HARVEST, MIDDLE, true, IntentCheck.fail("no_crop"), 0.6F)), harvest, 0.0F);

        assertEquals(EndReason.CONDITION_FAILED, choice.ended());
        assertEquals(WANDER, choice.next());
    }

    @Test
    void endsTheRunningIntentWhenTheDirectiveNoLongerPermitsIt() {
        RunningIntent fight = new RunningIntent(FIGHT, NOW - 5);
        Choice choice = choose(List.of(ok(WANDER, LOW, 0.05F),
            new Candidate(FIGHT, HIGH, false, IntentCheck.OK, 0.8F)), fight, 0.0F);

        assertEquals(EndReason.NOT_ALLOWED, choice.ended());
        assertEquals(WANDER, choice.next());
    }

    @Test
    void endsTheRunningIntentWhenItIsNoLongerOffered() {
        RunningIntent fight = new RunningIntent(FIGHT, NOW - 5);
        Choice choice = choose(List.of(ok(WANDER, LOW, 0.05F)), fight, 0.0F);

        assertEquals(EndReason.NOT_OFFERED, choice.ended());
        assertEquals(WANDER, choice.next());
    }

    @Test
    void anEndedIntentDoesNotCompeteInTheSameEvaluation() {
        RunningIntent harvest = new RunningIntent(HARVEST, NOW - 5);
        Choice choice = choose(List.of(new Candidate(HARVEST, MIDDLE, false, IntentCheck.OK, 0.6F)), harvest, 0.0F);

        assertEquals(EndReason.NOT_ALLOWED, choice.ended());
        assertNull(choice.next());
    }

    @Test
    void aPetThatHasEndedWhatItWasDoingTakesUpAnythingWhateverItsPriority() {
        // The floor is the running intent's priority only while it goes on.
        RunningIntent fight = new RunningIntent(FIGHT, NOW - 5);
        Choice choice = choose(List.of(new Candidate(FIGHT, HIGH, true, IntentCheck.fail("no_attack_target"), 0.8F),
            ok(WANDER, LOW, 0.05F)), fight, 0.0F);

        assertEquals(EndReason.CONDITION_FAILED, choice.ended());
        assertEquals(WANDER, choice.next());
    }

    // ---- priority and score from data -------------------------------------------

    @Test
    void anIntentCarriesThePriorityAndWeighedScoreOfItsSettings() {
        Personality personality = new Personality(Map.of(PetIntents.WANDER, 2.0F),
            Map.of(DayPhase.NIGHT, Map.of(PetIntents.WANDER, 3.0F)), 0.0F, List.of(), List.of(), IdleHabits.DEFAULT, Map.of());

        List<Candidate> day = farmCandidates(personality, DayPhase.DAY, targets(false, false, false), null);
        List<Candidate> night = farmCandidates(personality, DayPhase.NIGHT, targets(false, false, false), null);

        assertEquals(settings(PetIntents.WANDER).priority(), day.get(0).priority());
        assertEquals(settings(PetIntents.HARVEST).priority(), day.get(1).priority());
        assertEquals(0.1F, day.get(0).score(), 1.0E-6F);
        assertEquals(0.3F, night.get(0).score(), 1.0E-6F);
    }

    @Test
    void aPersonalityCannotMakeALowerPriorityOutrankAnExam() {
        // Wandering, scored as high as a personality may, against an exam.
        Personality restless = new Personality(Map.of(PetIntents.WANDER, 100.0F), Map.of(), 1.0F,
            List.of(), List.of(), IdleHabits.DEFAULT, Map.of());
        IntentContext ctx = TestContext.at(PET, TestAnchors.free(PET)).personality(restless)
            .examDesk(GlobalPos.of(OVERWORLD, PET.pos().east(2))).build();
        List<Candidate> candidates = IntentSelector.candidates(
            List.of(PetIntents.get(PetIntents.WANDER), PetIntents.get(PetIntents.TAKE_EXAM)), ctx, null, category -> true);

        assertTrue(candidates.get(0).score() > candidates.get(1).score());
        assertEquals(PetIntents.TAKE_EXAM, choose(candidates, null, 1.0F, FixedRandom.floats(1.0F, 0.0F)).next());
    }

    @Test
    void anIntentWithoutSettingsIsUnavailable() {
        Map<ResourceLocation, ErrandSettings<?>> shipped = ErrandSettingsData.all();
        Map<ResourceLocation, ErrandSettings<?>> withoutHarvest = new HashMap<>(shipped);
        withoutHarvest.remove(PetIntents.HARVEST);
        try {
            ErrandSettings.replaceAll(withoutHarvest);

            List<Candidate> candidates = farmCandidates(Personality.DEFAULT, DayPhase.DAY, targets(true, false, false), null);

            assertFalse(candidates.get(1).eligible());
            assertEquals(PetIntents.WANDER, choose(candidates, null, 0.0F).next());
        } finally {
            ErrandSettings.replaceAll(shipped);
        }
    }

    // ---- the farmer's order -----------------------------------------------------

    @Test
    void farmerOrderHoldsWhateverThePersonality() {
        // Multipliers that would put deliver over plant over harvest, and as much noise as a
        // personality may have.
        Personality inverted = new Personality(
            Map.of(PetIntents.HARVEST, 0.1F, PetIntents.PLANT, 5.0F, PetIntents.DELIVER, 10.0F),
            Map.of(DayPhase.DAY, Map.of(PetIntents.PLANT, 2.0F)), 1.0F, List.of(), List.of(), IdleHabits.DEFAULT, Map.of());

        for (float noise = 0.0F; noise <= 1.0F; noise += 0.125F) {
            Choice everything = choose(farmCandidates(inverted, DayPhase.DAY, targets(true, true, true), null), null,
                inverted.randomness(), FixedRandom.floats(noise, 1.0F - noise));
            assertEquals(PetIntents.HARVEST, everything.next(), everything::toString);
        }

        RunningIntent planting = new RunningIntent(PetIntents.PLANT, NOW - 5);
        Choice cropRipened = choose(farmCandidates(inverted, DayPhase.DAY, targets(true, true, true), PetIntents.PLANT),
            planting, inverted.randomness(), FixedRandom.floats(0.5F));
        assertEquals(EndReason.CONDITION_FAILED, cropRipened.ended());
        assertEquals(PetIntents.HARVEST, cropRipened.next());
    }

    @Test
    void plantWaitsWhileHarvestCanStart() {
        List<Candidate> bothReady = farmCandidates(Personality.DEFAULT, DayPhase.DAY, targets(true, true, false), null);
        List<Candidate> cropNotRipe = farmCandidates(Personality.DEFAULT, DayPhase.DAY, targets(false, true, false), null);

        assertTrue(bothReady.get(1).eligible());
        assertFalse(bothReady.get(2).eligible());
        assertEquals("intent.chiikawa.fail.higher_priority", bothReady.get(2).check().reasonKey());
        assertTrue(cropNotRipe.get(2).eligible());
    }

    @Test
    void deliverWaitsForHarvestAndPlant() {
        Candidate withCrop = farmCandidates(Personality.DEFAULT, DayPhase.DAY, targets(true, false, true), null).get(3);
        Candidate withFarmland = farmCandidates(Personality.DEFAULT, DayPhase.DAY, targets(false, true, true), null).get(3);
        Candidate withBoth = farmCandidates(Personality.DEFAULT, DayPhase.DAY, targets(true, true, true), null).get(3);
        Candidate withNeither = farmCandidates(Personality.DEFAULT, DayPhase.DAY, targets(false, false, true), null).get(3);

        assertEquals("intent.chiikawa.fail.higher_priority", withCrop.check().reasonKey());
        assertEquals("intent.chiikawa.fail.higher_priority", withFarmland.check().reasonKey());
        assertEquals("intent.chiikawa.fail.higher_priority", withBoth.check().reasonKey());
        assertTrue(withNeither.eligible());
    }

    @Test
    void anErrandOnlyWaitsForOneThatIsOffered() {
        // A crop is ripe, but harvest is not among the intents offered, say to a pet of another job.
        IntentContext ctx = TestContext.at(PET, TestAnchors.free(PET)).targets(targets(true, true, false)).build();
        List<Candidate> plantAlone = IntentSelector.candidates(List.of(PetIntents.get(PetIntents.PLANT)), ctx, null,
            category -> true);

        assertTrue(plantAlone.get(0).eligible());
    }

    // ---- slips ------------------------------------------------------------------

    @Test
    void workTheCarriedSlipCountsIsPreferred() {
        PetTask weeding = new PetTask(id("weeding"), id("farmer"), PetWorkCounters.WEED, PetTask.NO_ICON, 8,
            ResourceKey.create(Registries.LOOT_TABLE, id("pet_task/weeding")), 0);
        Optional<GlobalPos> near = near(true, 1);
        IntentContext ctx = TestContext.at(PET, TestAnchors.free(PET))
            .targets(new PerceivedTargets(Optional.empty(), near, Optional.empty(), Optional.empty(), near, Optional.empty(),
                Optional.empty()))
            .task(weeding)
            .build();
        List<Candidate> candidates = IntentSelector.candidates(
            List.of(PetIntents.get(PetIntents.HARVEST), PetIntents.get(PetIntents.WEED)), ctx, null, category -> true);

        assertEquals(candidates.get(0).priority(), candidates.get(1).priority());
        assertEquals(0.6F, candidates.get(0).score(), 1.0E-6F);
        assertEquals(0.5F + IntentSelector.TASK_BONUS, candidates.get(1).score(), 1.0E-6F);
        assertEquals(PetIntents.WEED, choose(candidates, null, 0.0F).next());
    }

    @Test
    void followingDoesNotTearAPetOffTheSlipItIsWorkingOff() {
        GlobalPos owner = GlobalPos.of(OVERWORLD, PET.pos().west(11));
        PetTask weeding = new PetTask(id("weeding"), id("farmer"), PetWorkCounters.WEED, PetTask.NO_ICON, 8,
            ResourceKey.create(Registries.LOOT_TABLE, id("pet_task/weeding")), 0);
        PerceivedTargets weeds = new PerceivedTargets(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            near(true, 1), Optional.empty(), Optional.empty());
        PetAnchor following = TestAnchors.following(owner);
        IntentContext ctx = TestContext.at(PET, following).targets(weeds).task(weeding).build();
        List<Candidate> candidates = IntentSelector.candidates(
            List.of(PetIntents.get(PetIntents.FOLLOW_OWNER), PetIntents.get(PetIntents.WEED)), ctx, PetIntents.WEED,
            category -> true);
        RunningIntent running = new RunningIntent(PetIntents.WEED, NOW - 5);

        Candidate follow = candidates.get(0);
        assertTrue(follow.eligible(), "the owner is far enough to call it");
        assertTrue(follow.score() > candidates.get(1).score(), "and the call outscores the weeding");
        assertTrue(choose(candidates, running, 0.0F).keeps(running));
    }

    // ---- a good meal ------------------------------------------------------------

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

    // ---- helpers ----------------------------------------------------------------

    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos PET = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));

    /** A free pet with a crop in sight, either freshly fed or not. */
    private static IntentContext fed(PerceivedTargets targets, boolean eager) {
        TestContext ctx = TestContext.at(PET, TestAnchors.free(PET)).targets(targets);
        return (eager ? ctx.eager() : ctx).build();
    }

    /** Wander, harvest, plant and deliver, as a free farmer sees them. */
    private static List<Candidate> farmCandidates(Personality personality, DayPhase phase, PerceivedTargets targets,
            ResourceLocation running) {
        IntentContext ctx = TestContext.at(PET, TestAnchors.free(PET))
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

    private static ErrandSettings<?> settings(ResourceLocation errand) {
        return ErrandSettings.of(errand).orElseThrow();
    }

    private static Choice choose(List<Candidate> candidates, RunningIntent current, float randomness) {
        return choose(candidates, current, randomness, FixedRandom.floats(0.5F));
    }

    private static Choice choose(List<Candidate> candidates, RunningIntent current, float randomness, RandomSource random) {
        return IntentSelector.choose(candidates, current, randomness, random);
    }

    private static Candidate ok(ResourceLocation id, int priority, float score) {
        return new Candidate(id, priority, true, IntentCheck.OK, score);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chiikawa", path);
    }
}
