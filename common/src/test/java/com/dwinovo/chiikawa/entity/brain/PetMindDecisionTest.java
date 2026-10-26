package com.dwinovo.chiikawa.entity.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Continuation;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Decision;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionTrace;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Permits;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Scored;
import com.dwinovo.chiikawa.entity.brain.errand.impl.BlockWorkErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.CombatErrand;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.dwinovo.chiikawa.testing.StubErrand;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestRests;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The rules a pet's mind decides by: a higher priority interrupts a lower one, an equal one never
 * does, the score and the noise only count among errands of one priority, and an errand that yields
 * to another waits for it only when it is about to start. The mind's side, the pet itself, is the
 * game tests'; this is the part that needs no world.
 */
class PetMindDecisionTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos PET = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final RestLedger NO_RESTS = TestRests.noRests();
    private static final Permits ALL = category -> true;
    private static final long NOW = 1000L;
    private static final int NONE = Integer.MIN_VALUE;
    private static final int LOW = 10;
    private static final int MIDDLE = 50;
    private static final int HIGH = 80;
    /** Pottering about is one way and nothing else, so that deciding to wander draws nothing among habits. */
    private static final IdleHabits PAUSE_ONLY = new IdleHabits(new IdleHabits.Glance(0, 5.0F, new InclusiveRange<>(45)),
        new IdleHabits.Glance(0, 5.0F, new InclusiveRange<>(45)), 0, new IdleHabits.Pause(1, new InclusiveRange<>(30, 60)));

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    // ---- filtering and ranking --------------------------------------------------

    @Test
    void picksTheHighestScoringCandidateOfTheHighestPriority() {
        Decision.Started started = decide(List.of(ErrandIds.WANDER, ErrandIds.SOCIALIZE, ErrandIds.PICK_UP_ITEM), partnerAndItem(), NONE,
            FixedRandom.floats()).orElseThrow();

        assertEquals(ErrandIds.PICK_UP_ITEM, started.type().id());
        assertEquals(List.of(ErrandIds.PICK_UP_ITEM, ErrandIds.SOCIALIZE), started.ranking().stream().map(Scored::errand).toList());
    }

    @Test
    void aHigherPriorityWinsWhateverItsScore() {
        // A farmer who thinks the world of harvesting still drops it for a fight.
        Personality keen = new Personality(Map.of(ErrandIds.HARVEST, 100.0F), Map.of(), 0.0F, List.of(), List.of(),
            PAUSE_ONLY, Map.of());
        TestView view = view().personality(keen).targets(fightAndCrop());

        assertEquals(ErrandIds.MELEE, decide(List.of(ErrandIds.HARVEST, ErrandIds.MELEE), view, NONE, FixedRandom.floats())
            .orElseThrow().type().id());
    }

    @Test
    void skipsCandidatesTheDirectiveForbidsOrWhoseConditionFails() {
        TestView view = view().targets(foe());
        Permits noFighting = category -> category != ErrandCategory.COMBAT;

        Optional<Decision.Started> started = Decision.decide(offered(List.of(ErrandIds.WANDER, ErrandIds.MELEE, ErrandIds.HARVEST)), view,
            noFighting, NO_RESTS, NONE, FixedRandom.floats(), NOW, null);

        assertEquals(ErrandIds.WANDER, started.orElseThrow().type().id());
        assertEquals(1, started.orElseThrow().ranking().size());
    }

    @Test
    void returnsNothingWhenNoCandidateCanRun() {
        assertEquals(Optional.empty(), decide(List.of(ErrandIds.MELEE, ErrandIds.HARVEST), view(), NONE, FixedRandom.floats()));
    }

    // ---- noise ------------------------------------------------------------------

    @Test
    void noiseOnlyDecidesBetweenCandidatesOfOnePriority() {
        // nextFloat() == 1.0 gives the full +randomness, 0.0 the full -randomness.
        TestView view = partnerAndItem().personality(noisy(0.1F));
        List<ResourceLocation> offered = List.of(ErrandIds.SOCIALIZE, ErrandIds.PICK_UP_ITEM);

        assertEquals(ErrandIds.SOCIALIZE, decide(offered, view, NONE, FixedRandom.floats(1.0F, 0.0F)).orElseThrow().type().id());
        assertEquals(ErrandIds.PICK_UP_ITEM, decide(offered, view, NONE, FixedRandom.floats(0.0F, 1.0F)).orElseThrow().type().id());
    }

    @Test
    void noiseNeverLiftsALowerPriorityOverAHigherOne() {
        // The most noise a pet can have, all of it in favour of wandering, which it also fancies.
        Personality restless = new Personality(Map.of(ErrandIds.WANDER, 100.0F), Map.of(), 1.0F, List.of(), List.of(),
            PAUSE_ONLY, Map.of());
        TestView view = view().personality(restless).targets(farm(true, false, false));
        List<ResourceLocation> offered = List.of(ErrandIds.WANDER, ErrandIds.HARVEST);

        assertEquals(ErrandIds.HARVEST, decide(offered, view, NONE, FixedRandom.floats(1.0F, 0.0F)).orElseThrow().type().id());
        assertEquals(ErrandIds.HARVEST, decide(offered, view, NONE, FixedRandom.floats(0.0F, 1.0F)).orElseThrow().type().id());
    }

    @Test
    void nothingIsDrawnWhereNothingIsUndecided() {
        // FixedRandom throws on a draw it has no value for.
        TestView crop = view().personality(noisy(1.0F)).targets(farm(true, false, false));
        assertEquals(ErrandIds.HARVEST, decide(List.of(ErrandIds.WANDER, ErrandIds.HARVEST), crop, NONE, FixedRandom.floats())
            .orElseThrow().type().id());
        assertEquals(ErrandIds.PICK_UP_ITEM, decide(List.of(ErrandIds.SOCIALIZE, ErrandIds.PICK_UP_ITEM), partnerAndItem(), NONE,
            FixedRandom.floats()).orElseThrow().type().id());
    }

    // ---- keeping the running errand ---------------------------------------------

    @Test
    void anEqualPriorityNeverInterrupts() {
        // Planting while the crop beside it ripens: harvest is of its priority and scores more.
        TestView view = view().targets(farm(true, true, false));
        List<ResourceLocation> offered = List.of(ErrandIds.PLANT, ErrandIds.HARVEST);

        // However far the other scores, and whatever the noise.
        for (float randomness : new float[] {0.0F, 0.3F, 1.0F}) {
            TestView noisy = view.personality(noisy(randomness));
            assertEquals(Optional.empty(), decide(offered, noisy, MIDDLE, FixedRandom.floats(0.0F, 1.0F)));
            assertEquals(Optional.empty(), decide(offered, noisy, MIDDLE, FixedRandom.floats(1.0F, 0.0F)));
        }
    }

    @Test
    void aRunningErrandIsNotEndedBecauseOneItYieldsToCouldStart() {
        TestView ripe = view().targets(farm(true, true, false));
        Plan planting = TestErrands.planOf(ErrandIds.PLANT, ripe);

        // Starting plant waits for harvest; going on with plant does not.
        assertEquals(Optional.empty(), ending(ErrandIds.PLANT, planting, ripe));
        assertEquals(ErrandIds.HARVEST, decide(List.of(ErrandIds.PLANT, ErrandIds.HARVEST), ripe, NONE, FixedRandom.floats())
            .orElseThrow().type().id());
    }

    @Test
    void aHigherPriorityInterruptsAtOnceWhateverTheNoise() {
        // Wandering draws the most positive noise, the fight the most negative.
        TestView view = view().personality(noisy(0.3F)).targets(foe());
        Optional<Decision.Started> started = decide(List.of(ErrandIds.WANDER, ErrandIds.MELEE), view, LOW, FixedRandom.floats(1.0F, 0.0F));

        assertEquals(ErrandIds.MELEE, started.orElseThrow().type().id());
    }

    @Test
    void aLowerPriorityNeverInterrupts() {
        TestView view = view().targets(foe());

        assertEquals(Optional.empty(), decide(List.of(ErrandIds.MELEE, ErrandIds.WANDER, ErrandIds.PICK_UP_ITEM), view, HIGH, FixedRandom.floats()));
    }

    @Test
    void aRandomPetDoesNotFlipBetweenEqualErrands() {
        TestView view = partnerAndItem().personality(noisy(0.3F));
        List<ResourceLocation> equal = List.of(ErrandIds.SOCIALIZE, ErrandIds.PICK_UP_ITEM);
        ResourceLocation current = null;
        int switches = 0;
        for (int evaluation = 0; evaluation < 50; evaluation++) {
            // Alternate which one the noise favours on every evaluation.
            RandomSource random = evaluation % 2 == 0 ? FixedRandom.floats(1.0F, 0.0F) : FixedRandom.floats(0.0F, 1.0F);
            Optional<Decision.Started> started = decide(equal, view, current == null ? NONE : priorityOf(current), random);
            if (started.isPresent()) {
                current = started.get().type().id();
                switches++;
            }
        }

        assertEquals(1, switches);
    }

    // ---- ending the running errand ----------------------------------------------

    @Test
    void endsTheRunningErrandWhenItsConditionFails() {
        TestView cropGone = view();

        assertEquals(Optional.of(Outcome.gaveUp("no_crop")), ending(ErrandIds.HARVEST, new BlockWorkErrand.Work(east(1)), cropGone));
    }

    @Test
    void theErrandAPetWasJustOnIsTheOneItIsOnOrTheOneThatEndedInTheTickBefore() {
        DecisionLog log = new DecisionLog();
        Running running = new Running(TestErrands.errand(ErrandIds.RANGED), new CombatErrand.Duel(UUID.randomUUID()),
            ErrandSettings.of(ErrandIds.RANGED).orElseThrow(), 0, NOW - 5, Set.of());

        assertNull(PetMind.justBefore(Optional.empty(), log, NOW));
        assertEquals(ErrandIds.RANGED, PetMind.justBefore(Optional.of(running), log, NOW).id());

        // A fight that ended by itself, such as for a foe put down, leaves the pet on none: it is the fight it was on.
        log.ended(NOW - 1, ErrandIds.MELEE, Outcome.done(), 30);
        assertEquals(ErrandIds.MELEE, PetMind.justBefore(Optional.empty(), log, NOW).id());
        assertEquals(ErrandIds.RANGED, PetMind.justBefore(Optional.of(running), log, NOW).id());
        // And not one that ended a while ago, since the pet has been doing other things since.
        assertNull(PetMind.justBefore(Optional.empty(), log, NOW + 5));
    }

    @Test
    void anErrandGoesOnAfterAReloadMadeNewTypesOfEveryErrandAsTheTypeItStartedAs() {
        // The pet is on an errand that wants to go on. The reload has made a type of it that would not, and the mind offers that.
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("chiikawa", "work");
        ErrandType<?, ?> started = new StubErrand(id);
        ErrandType<?, ?> reloaded = new StubErrand(id, Check.fail("gone"));
        Running running = new Running(started, new Plan() {
        }, TestRests.settings(1200, TestRests.ticks(0, 0, 0)), 0, NOW - 5, Set.of());

        assertTrue(PetMind.isOffered(List.of(reloaded), running.errand()), "by id, not by the type it started as");
        assertFalse(PetMind.isOffered(List.of(reloaded), ErrandIds.WANDER));
        assertEquals(Optional.empty(), Continuation.endOf(running, PetMind.isOffered(List.of(reloaded), running.errand()), view(), ALL));
        assertSame(started, PetMind.justBefore(Optional.of(running), new DecisionLog(), NOW));
    }

    @Test
    void aPetThatHasEndedWhatItWasDoingTakesUpAnythingWhateverItsPriority() {
        // The floor is the running errand's priority only while it goes on.
        TestView view = view();
        Plan shooting = new CombatErrand.Duel(UUID.randomUUID());

        assertEquals(Optional.of(Outcome.gaveUp("no_arrows")), ending(ErrandIds.RANGED, shooting, view));
        assertEquals(ErrandIds.WANDER, decide(List.of(ErrandIds.RANGED, ErrandIds.WANDER), view, NONE, FixedRandom.floats())
            .orElseThrow().type().id());
    }

    // ---- priority and score from data -------------------------------------------

    @Test
    void anErrandCarriesThePriorityAndWeighedScoreOfItsSettings() {
        Personality personality = new Personality(Map.of(ErrandIds.WANDER, 2.0F),
            Map.of(DayPhase.NIGHT, Map.of(ErrandIds.WANDER, 3.0F)), 0.0F, List.of(), List.of(), PAUSE_ONLY, Map.of());
        List<ResourceLocation> offered = List.of(ErrandIds.WANDER, ErrandIds.HARVEST);
        TestView day = view().personality(personality).phase(DayPhase.DAY);
        TestView night = view().personality(personality).phase(DayPhase.NIGHT);

        DecisionTrace byDay = look(offered, day);
        DecisionTrace byNight = look(offered, night);

        assertEquals(ErrandSettings.of(ErrandIds.WANDER).orElseThrow().priority(), priorityOf(byDay, ErrandIds.WANDER));
        assertEquals(0.1F, scores(byDay).get(ErrandIds.WANDER), 1.0E-6F);
        assertEquals(0.3F, scores(byNight).get(ErrandIds.WANDER), 1.0E-6F);
    }

    @Test
    void aPersonalityCannotMakeALowerPriorityOutrankAnExam() {
        // Wandering, scored as high as a personality may, against an exam.
        Personality restless = new Personality(Map.of(ErrandIds.WANDER, 100.0F), Map.of(), 1.0F,
            List.of(), List.of(), PAUSE_ONLY, Map.of());
        TestView view = view().personality(restless).examDesk(GlobalPos.of(OVERWORLD, PET.pos().east(2)));
        List<ResourceLocation> offered = List.of(ErrandIds.WANDER, ErrandIds.TAKE_EXAM);

        Map<ResourceLocation, Float> scores = scores(look(offered, view));
        assertTrue(scores.get(ErrandIds.WANDER) > scores.get(ErrandIds.TAKE_EXAM));
        assertEquals(ErrandIds.TAKE_EXAM, decide(offered, view, NONE, FixedRandom.floats(1.0F, 0.0F)).orElseThrow().type().id());
    }

    @Test
    void anErrandWithoutSettingsIsSwitchedOff() {
        Map<ResourceLocation, ErrandSettings<?>> shipped = ErrandSettingsData.all();
        Map<ResourceLocation, ErrandSettings<?>> withoutHarvest = new HashMap<>(shipped);
        withoutHarvest.remove(ErrandIds.HARVEST);
        try {
            ErrandSettings.replaceAll(withoutHarvest);
            TestView view = view().targets(farm(true, false, false));

            assertEquals(ErrandIds.WANDER, decide(List.of(ErrandIds.WANDER, ErrandIds.HARVEST), view, NONE, FixedRandom.floats())
                .orElseThrow().type().id());
            assertEquals(DecisionTrace.SWITCHED_OFF, verdicts(look(List.of(ErrandIds.WANDER, ErrandIds.HARVEST), view)).get(ErrandIds.HARVEST));
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
            Map.of(ErrandIds.HARVEST, 0.1F, ErrandIds.PLANT, 5.0F, ErrandIds.DELIVER, 10.0F),
            Map.of(DayPhase.DAY, Map.of(ErrandIds.PLANT, 2.0F)), 1.0F, List.of(), List.of(), PAUSE_ONLY, Map.of());
        TestView everything = view().personality(inverted).targets(farm(true, true, true));

        for (float noise = 0.0F; noise <= 1.0F; noise += 0.125F) {
            Optional<Decision.Started> started = decide(farm(), everything, NONE, FixedRandom.floats(noise, 1.0F - noise));
            assertEquals(ErrandIds.HARVEST, started.orElseThrow().type().id());
        }
    }

    @Test
    void plantWaitsWhileHarvestCanStart() {
        Map<ResourceLocation, String> bothReady = verdicts(look(farm(), view().targets(farm(true, true, false))));
        Map<ResourceLocation, String> cropNotRipe = verdicts(look(farm(), view().targets(farm(false, true, false))));

        assertEquals(DecisionTrace.STARTABLE, bothReady.get(ErrandIds.HARVEST));
        assertEquals("intent.chiikawa.fail.higher_priority", bothReady.get(ErrandIds.PLANT));
        assertEquals(DecisionTrace.STARTABLE, cropNotRipe.get(ErrandIds.PLANT));
    }

    @Test
    void deliverWaitsForHarvestAndPlant() {
        String withCrop = verdicts(look(farm(), view().targets(farm(true, false, true)))).get(ErrandIds.DELIVER);
        String withFarmland = verdicts(look(farm(), view().targets(farm(false, true, true)))).get(ErrandIds.DELIVER);
        String withBoth = verdicts(look(farm(), view().targets(farm(true, true, true)))).get(ErrandIds.DELIVER);
        String withNeither = verdicts(look(farm(), view().targets(farm(false, false, true)))).get(ErrandIds.DELIVER);

        assertEquals("intent.chiikawa.fail.higher_priority", withCrop);
        assertEquals("intent.chiikawa.fail.higher_priority", withFarmland);
        assertEquals("intent.chiikawa.fail.higher_priority", withBoth);
        assertEquals(DecisionTrace.STARTABLE, withNeither);
    }

    @Test
    void anErrandOnlyWaitsForOneThatIsOffered() {
        // A crop is ripe, but harvest is not among the errands offered, say to a pet of another job.
        TestView view = view().targets(farm(true, true, false));

        assertEquals(ErrandIds.PLANT, decide(List.of(ErrandIds.PLANT), view, NONE, FixedRandom.floats()).orElseThrow().type().id());
    }

    @Test
    void anUnreachableCropDoesNotHoldPlantingBack() {
        // Harvest cannot start for a crop beyond the area, so it holds nothing back.
        TestView view = view().targets(new PerceivedTargets(Optional.empty(), Optional.of(east(30)), Optional.of(east(2)),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        assertEquals(ErrandIds.PLANT, decide(List.of(ErrandIds.HARVEST, ErrandIds.PLANT), view, NONE, FixedRandom.floats())
            .orElseThrow().type().id());
    }

    // ---- slips ------------------------------------------------------------------

    @Test
    void workTheCarriedSlipCountsIsPreferred() {
        PerceivedTargets targets = new PerceivedTargets(Optional.empty(), Optional.of(east(1)), Optional.empty(), Optional.empty(),
            Optional.of(east(1)), Optional.empty(), Optional.empty());
        TestView view = view().targets(targets).slip(weeding());
        List<ResourceLocation> offered = List.of(ErrandIds.HARVEST, ErrandIds.WEED);

        Map<ResourceLocation, Float> scores = scores(look(offered, view));

        assertEquals(0.6F, scores.get(ErrandIds.HARVEST), 1.0E-6F);
        assertEquals(0.5F + Decision.TASK_BONUS, scores.get(ErrandIds.WEED), 1.0E-6F);
        assertEquals(ErrandIds.WEED, decide(offered, view, NONE, FixedRandom.floats()).orElseThrow().type().id());
    }

    @Test
    void followingDoesNotTearAPetOffTheSlipItIsWorkingOff() {
        GlobalPos owner = GlobalPos.of(OVERWORLD, PET.pos().west(11));
        PerceivedTargets weeds = new PerceivedTargets(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(east(-4)), Optional.empty(), Optional.empty());
        TestView view = TestView.at(PET, TestAnchors.following(owner)).targets(weeds).slip(weeding());
        List<ResourceLocation> offered = List.of(ErrandIds.FOLLOW_OWNER, ErrandIds.WEED);

        Map<ResourceLocation, Float> scores = scores(look(offered, view));
        assertTrue(scores.containsKey(ErrandIds.FOLLOW_OWNER), "the owner is far enough to call it");
        assertTrue(scores.get(ErrandIds.FOLLOW_OWNER) > scores.get(ErrandIds.WEED), "and the call outscores the weeding");
        assertEquals(Optional.empty(), decide(offered, view, MIDDLE, FixedRandom.floats()), "but it is of the same priority");
    }

    // ---- a good meal ------------------------------------------------------------

    @Test
    void aMealTipsTheScalesTowardsWorkAndNothingElse() {
        TestView crop = view().targets(farm(true, false, false));
        List<ResourceLocation> offered = List.of(ErrandIds.WANDER, ErrandIds.HARVEST);

        Map<ResourceLocation, Float> hungry = scores(look(offered, crop));
        Map<ResourceLocation, Float> full = scores(look(offered, view().targets(crop.targets()).fed()));

        assertEquals(hungry.get(ErrandIds.WANDER), full.get(ErrandIds.WANDER), 1.0E-6F);
        assertEquals(hungry.get(ErrandIds.HARVEST) + Decision.EAGER_BONUS, full.get(ErrandIds.HARVEST), 1.0E-6F);
    }

    @Test
    void aMealIsNotWorthAsMuchAsTheSlipThePetCarries() {
        assertTrue(Decision.EAGER_BONUS < Decision.TASK_BONUS);
    }

    @Test
    void lookingDrawsNothingAndPicksNothing() {
        DecisionTrace trace = new DecisionTrace();
        TestView view = partnerAndItem().personality(noisy(1.0F));

        // FixedRandom throws on any draw; and with none given nothing is picked, but every priority is gone through.
        assertEquals(Optional.empty(), Decision.decide(offered(List.of(ErrandIds.WANDER, ErrandIds.SOCIALIZE, ErrandIds.PICK_UP_ITEM)), view,
            ALL, NO_RESTS, NONE, null, NOW, trace));
        assertNull(trace.pick().orElse(null));
        assertEquals(3, trace.lines().size());
        assertFalse(scores(trace).isEmpty());
    }

    // ---- helpers ----------------------------------------------------------------

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, PET.pos().east(blocks));
    }

    private static TestView view() {
        return TestView.at(PET, TestAnchors.free(PET)).personality(noisy(0.0F));
    }

    /** A free pet with an item to pick up and a pet to call on. */
    private static TestView partnerAndItem() {
        return view().socialPartner(east(3))
            .targets(new PerceivedTargets(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(new PickableItem(UUID.randomUUID(), east(2)))));
    }

    /** A crop to harvest, farmland to plant and a chest to fill, each as near as the pet may go. */
    private static PerceivedTargets farm(boolean crop, boolean farmland, boolean container) {
        return new PerceivedTargets(Optional.empty(), crop ? Optional.of(east(1)) : Optional.empty(),
            farmland ? Optional.of(east(2)) : Optional.empty(), container ? Optional.of(east(3)) : Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** Something to fight. */
    private static PerceivedTargets foe() {
        return new PerceivedTargets(Optional.of(new AttackTarget(UUID.randomUUID(), east(2))), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static PerceivedTargets fightAndCrop() {
        return new PerceivedTargets(Optional.of(new AttackTarget(UUID.randomUUID(), east(2))), Optional.of(east(1)), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static Personality noisy(float randomness) {
        return new Personality(Map.of(), Map.of(), randomness, List.of(), List.of(), PAUSE_ONLY, Map.of());
    }

    private static List<ResourceLocation> farm() {
        return List.of(ErrandIds.WANDER, ErrandIds.HARVEST, ErrandIds.PLANT, ErrandIds.DELIVER);
    }

    private static List<ErrandType<?, ?>> offered(List<ResourceLocation> ids) {
        return ids.stream().<ErrandType<?, ?>>map(TestErrands::errand).toList();
    }

    private static Optional<Decision.Started> decide(List<ResourceLocation> ids, PetView view, int floor, RandomSource random) {
        return Decision.decide(offered(ids), view, ALL, NO_RESTS, floor, random, NOW, null);
    }

    /** How every errand fares: nothing is picked and every priority is gone through. */
    private static DecisionTrace look(List<ResourceLocation> ids, PetView view) {
        DecisionTrace trace = new DecisionTrace();
        Decision.decide(offered(ids), view, ALL, NO_RESTS, NONE, null, NOW, trace);
        return trace;
    }

    private static Map<ResourceLocation, Float> scores(DecisionTrace trace) {
        Map<ResourceLocation, Float> scores = new HashMap<>();
        trace.lines().forEach(line -> line.score().ifPresent(score -> scores.put(line.errand(), score)));
        return scores;
    }

    private static Map<ResourceLocation, String> verdicts(DecisionTrace trace) {
        Map<ResourceLocation, String> verdicts = new HashMap<>();
        trace.lines().forEach(line -> verdicts.put(line.errand(), line.verdict()));
        return verdicts;
    }

    private static int priorityOf(DecisionTrace trace, ResourceLocation errand) {
        return trace.lines().stream().filter(line -> line.errand().equals(errand)).findFirst().orElseThrow().priority();
    }

    private static int priorityOf(ResourceLocation errand) {
        return ErrandSettings.of(errand).orElseThrow().priority();
    }

    private static Optional<Outcome> ending(ResourceLocation id, Plan plan, PetView view) {
        Running running = new Running(TestErrands.errand(id), plan, ErrandSettings.of(id).orElseThrow(), 0, NOW - 5, Set.of());
        return Continuation.endOf(running, true, view, ALL);
    }

    private static PetTask weeding() {
        return new PetTask(ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding"),
            ResourceLocation.fromNamespaceAndPath("chiikawa", "farmer"), PetWorkCounters.WEED, PetTask.NO_ICON, 8,
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("chiikawa", "pet_task/weeding")), 0);
    }
}
