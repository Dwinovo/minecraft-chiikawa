package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.errand.overlay.Overlays;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What the generated errand settings say: that every errand has them, and that they are the order the pets keep. */
class ErrandSettingsDataTest {
    /** 0.1.4 implementation plan, "Default priorities". */
    private static final Map<ResourceLocation, Integer> PRIORITIES = Map.ofEntries(
        Map.entry(ErrandIds.GIFT_OWNER, 80),
        Map.entry(WorkData.MELEE, 80),
        Map.entry(WorkData.RANGED, 80),
        Map.entry(ErrandIds.TAKE_EXAM, 80),
        Map.entry(ErrandIds.CHECK_RESULTS, 80),
        Map.entry(ErrandIds.COOPERATE, 80),
        Map.entry(ErrandIds.TAKE_TASK, 50),
        Map.entry(ErrandIds.SHOP, 50),
        Map.entry(WorkData.HARVEST, 50),
        Map.entry(WorkData.PLANT, 50),
        Map.entry(WorkData.DELIVER, 50),
        Map.entry(WorkData.WEED, 50),
        Map.entry(WorkData.PICK_MUSHROOM, 50),
        Map.entry(WorkData.PLAY_MUSIC, 50),
        Map.entry(ErrandIds.ANSWER_WHISTLE, 50),
        Map.entry(ErrandIds.FOLLOW_OWNER, 50),
        Map.entry(ErrandIds.TAKE_PHOTO, 40),
        Map.entry(ErrandIds.PICK_UP_ITEM, 30),
        Map.entry(ErrandIds.SOCIALIZE, 30),
        Map.entry(ErrandIds.WANDER, 10),
        Map.entry(ErrandIds.STAY, 10));

    /** The scores the intents have today, which the scores here must not move. */
    private static final Map<ResourceLocation, Float> SCORES = Map.ofEntries(
        Map.entry(ErrandIds.GIFT_OWNER, 0.85F),
        Map.entry(WorkData.MELEE, 0.8F),
        Map.entry(WorkData.RANGED, 0.8F),
        Map.entry(ErrandIds.TAKE_EXAM, 0.72F),
        Map.entry(ErrandIds.CHECK_RESULTS, 0.72F),
        Map.entry(ErrandIds.COOPERATE, 0.5F),
        Map.entry(ErrandIds.TAKE_TASK, 0.7F),
        Map.entry(ErrandIds.SHOP, 0.65F),
        Map.entry(WorkData.HARVEST, 0.6F),
        Map.entry(WorkData.PLANT, 0.6F),
        Map.entry(WorkData.DELIVER, 0.6F),
        Map.entry(WorkData.WEED, 0.5F),
        Map.entry(WorkData.PICK_MUSHROOM, 0.5F),
        Map.entry(WorkData.PLAY_MUSIC, 0.6F),
        Map.entry(ErrandIds.ANSWER_WHISTLE, 0.55F),
        Map.entry(ErrandIds.FOLLOW_OWNER, 0.5F),
        Map.entry(ErrandIds.TAKE_PHOTO, 0.45F),
        Map.entry(ErrandIds.PICK_UP_ITEM, 0.4F),
        Map.entry(ErrandIds.SOCIALIZE, 0.35F),
        Map.entry(ErrandIds.WANDER, 0.05F),
        Map.entry(ErrandIds.STAY, 1.0F));

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void theTableOfPrioritiesIsComplete() {
        Set<ResourceLocation> errands = ErrandSettingsData.all().keySet();

        assertEquals(errands, PRIORITIES.keySet());
        assertEquals(errands, SCORES.keySet());
    }

    @Test
    void thePrioritiesAreThePlans() {
        ErrandSettingsData.all().forEach((errand, settings) ->
            assertEquals(PRIORITIES.get(errand), settings.priority(), errand.toString()));
    }

    @Test
    void theScoresAreTheOnesThePetsHave() {
        ErrandSettingsData.all().forEach((errand, settings) ->
            assertEquals(SCORES.get(errand), settings.score(), errand.toString()));
    }

    @Test
    void everyFileIsOneThatLoads() {
        ErrandSettingsData.all().forEach((errand, settings) -> {
            var json = ErrandSettings.codecOf(errand).encodeStart(JsonOps.INSTANCE, settings).getOrThrow();

            assertEquals(settings, ErrandSettings.codecOf(errand).parse(JsonOps.INSTANCE, json).getOrThrow(), errand.toString());
            assertEquals(Optional.empty(), settings.problemAmong(errand, ErrandSettingsData.all()), errand.toString());
            assertEquals(Optional.empty(), settings.problemFor(Errands.get(errand)), errand.toString());
            assertTrue(settings.maxTicks() > 0, errand.toString());
            assertTrue(settings.rests().gaveUp().lasts(), errand + " must rest after giving up");
        });
    }

    @Test
    void theFarmersOrderIsDataAndNothingElseYields() {
        Map<ResourceLocation, List<ResourceLocation>> yields = new HashMap<>();
        ErrandSettingsData.all().forEach((errand, settings) -> {
            if (!settings.yieldsTo().isEmpty()) {
                yields.put(errand, settings.yieldsTo());
            }
        });

        assertEquals(Map.of(
            WorkData.PLANT, List.of(WorkData.HARVEST),
            WorkData.DELIVER, List.of(WorkData.HARVEST, WorkData.PLANT)), yields);
    }

    @Test
    void onlyTheFightsShoutOnSettingOff() {
        Map<ResourceLocation, Optional<VoiceMoment>> voices = new HashMap<>();
        ErrandSettingsData.all().forEach((errand, settings) -> {
            if (settings.startVoice().isPresent()) {
                voices.put(errand, settings.startVoice());
            }
        });

        assertEquals(Map.of(WorkData.MELEE, Optional.of(VoiceMoment.HUNT), WorkData.RANGED, Optional.of(VoiceMoment.HUNT)), voices);
    }

    @Test
    void onlyWanderingChatsOnTheSide() {
        Map<ResourceLocation, List<ResourceLocation>> overlays = new HashMap<>();
        ErrandSettingsData.all().forEach((errand, settings) -> {
            if (!settings.overlays().isEmpty()) {
                overlays.put(errand, settings.overlays());
            }
        });

        assertEquals(Map.of(ErrandIds.WANDER, List.of(Overlays.IDLE_CHATTER)), overlays);
    }

    @Test
    void aHabitOfAPetWithNothingToDoIsBoundOnlyByTheLongestHabitThereCanBe() {
        Map<ResourceLocation, ErrandSettings<?>> all = ErrandSettingsData.all();

        for (ResourceLocation errand : List.of(ErrandIds.WANDER, ErrandIds.STAY)) {
            int limit = all.get(errand).maxTicks();
            assertTrue(limit > IdleHabits.LONGEST_TICKS, errand + " would cut the longest habit short");
            assertTrue(limit <= IdleHabits.LONGEST_TICKS + 600, errand + " is no longer a safety bound on one habit");
        }
    }

    @Test
    void theRestsAreWhatEachErrandHasAlwaysDone() {
        Map<ResourceLocation, ErrandSettings<?>> all = ErrandSettingsData.all();

        assertEquals(600, ticks(all, ErrandIds.GIFT_OWNER));
        assertEquals(600, ticks(all, ErrandIds.SHOP));
        assertEquals(200, ticks(all, ErrandIds.TAKE_EXAM));
        assertEquals(1200, ticks(all, ErrandIds.CHECK_RESULTS));
        assertEquals(200, ticks(all, WorkData.HARVEST));
        assertEquals(40, ticks(all, ErrandIds.PICK_UP_ITEM));
        assertEquals(20, ticks(all, WorkData.MELEE));
        for (ResourceLocation errand : List.of(ErrandIds.FOLLOW_OWNER, ErrandIds.STAY, ErrandIds.WANDER)) {
            assertEquals(20, ticks(all, errand), errand.toString());
            assertEquals(RestSpec.NONE, all.get(errand).rests().done(), errand.toString());
            assertEquals(RestSpec.NONE, all.get(errand).rests().interrupted(), errand.toString());
        }
        assertEquals(600, ticks(all, ErrandIds.TAKE_TASK));
        assertEquals(600, all.get(ErrandIds.TAKE_TASK).rests().interrupted().amount().left().orElseThrow());
        assertEquals(Set.of("taken", "stored", "failed"), all.get(ErrandIds.TAKE_PHOTO).rests().reports().keySet());
    }

    @Test
    void aSceneRestsTheFullCooldownFromTheMomentItBeginsAndOnlyBrieflyIfItNeverDid() {
        for (ResourceLocation errand : List.of(ErrandIds.SOCIALIZE, ErrandIds.COOPERATE)) {
            ErrandSettings.Rests rests = ErrandSettingsData.all().get(errand).rests();

            // Past the start every way out has been rested by the report; before it, only the guard against asking again at once.
            assertEquals(RestSpec.NONE, rests.done(), errand.toString());
            assertEquals(RestSpec.NONE, rests.interrupted(), errand.toString());
            assertEquals(RestSpec.ticksPerSubject(100), rests.gaveUp(), errand.toString());
            assertEquals(Map.of("played", RestSpec.fromPerSubject(RestSources.SCENE_COOLDOWN)), rests.reports(), errand.toString());
        }
    }

    private static int ticks(Map<ResourceLocation, ErrandSettings<?>> all, ResourceLocation errand) {
        return all.get(errand).rests().gaveUp().amount().left().orElseThrow();
    }
}
