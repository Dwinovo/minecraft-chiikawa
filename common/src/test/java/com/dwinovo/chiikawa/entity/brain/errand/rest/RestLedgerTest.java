package com.dwinovo.chiikawa.entity.brain.errand.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.testing.StubErrand;
import com.dwinovo.chiikawa.testing.TestRests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** How long an errand rests once it has ended, and what a report that comes later does to it. */
class RestLedgerTest {
    private static final ResourceLocation PHOTO = id("take_photo");
    private static final ResourceLocation PLANT = id("plant");
    private static final ResourceLocation CAMERA = id("camera");
    private static final long NOW = 1000L;
    private static final int UPLOAD_WAIT = 600;
    private static final int CAMERA_COOLDOWN = 12000;
    private static final int RETRY = 300;
    private static final int SCENE_COOLDOWN = 24000;
    private static final int GIFT_COOLDOWN = 24000;
    private static final int SCENE_GUARD = 100;

    private final TestRests.Store store = new TestRests.Store();
    private final List<String> errors = new ArrayList<>();
    private ErrandSettings<String> photo;
    private RestLedger ledger;

    @BeforeEach
    void setUp() {
        RestLedger.forgetReported();
        photo = TestRests.settings(1200, new ErrandSettings.Rests(RestSpec.ticks(UPLOAD_WAIT), RestSpec.ticks(RETRY),
            RestSpec.ticks(0), Map.of("stored", RestSpec.ticks(CAMERA_COOLDOWN), "failed", RestSpec.ticks(RETRY))));
        ledger = ledger(photo);
    }

    @Test
    void anErrandThatEndsRestsAsItsSettingsSayForHowItEnded() {
        ledger.afterOutcome(running(PHOTO, photo, new Plan() {
        }), Outcome.done(), NOW);
        assertEquals(UPLOAD_WAIT, ledger.remaining(PHOTO, NOW));
        assertTrue(ledger.resting(PHOTO, NOW));
        assertFalse(ledger.resting(PHOTO, NOW + UPLOAD_WAIT), "the rest is over when its time is");
    }

    @Test
    void eachWayAnErrandEndsHasItsOwnRest() {
        ledger.afterOutcome(running(PHOTO, photo, new Plan() {
        }), Outcome.gaveUp("no"), NOW);

        assertEquals(RETRY, ledger.remaining(PHOTO, NOW));
    }

    @Test
    void anErrandThatEndsNeverShortensALongerRestItIsAlreadyOn() {
        // A photo stored while the camera is still up has rested the pet for the camera's cooldown.
        ledger.afterReport(PHOTO, "stored", RestContext.NONE, NOW);

        ledger.afterOutcome(running(PHOTO, photo, new Plan() {
        }), Outcome.done(), NOW + 10);

        assertEquals(CAMERA_COOLDOWN - 10, ledger.remaining(PHOTO, NOW + 10), "the wait for the upload did not cut it short");
    }

    @Test
    void anErrandThatEndsLengthensAShorterRest() {
        ledger.afterReport(PHOTO, "failed", RestContext.NONE, NOW);

        ledger.afterOutcome(running(PHOTO, photo, new Plan() {
        }), Outcome.done(), NOW);

        assertEquals(UPLOAD_WAIT, ledger.remaining(PHOTO, NOW));
    }

    @Test
    void anEndThatRestsForNothingChangesNothing() {
        ledger.afterReport(PHOTO, "stored", RestContext.NONE, NOW);

        ledger.afterOutcome(running(PHOTO, photo, new Plan() {
        }), Outcome.interrupted("higher"), NOW);

        assertEquals(CAMERA_COOLDOWN, ledger.remaining(PHOTO, NOW));
        assertEquals(1, store.writes());
    }

    @Test
    void aReportReplacesWhatTheErrandWasRestingEvenWithLess() {
        ledger.afterReport(PHOTO, "stored", RestContext.NONE, NOW);
        assertEquals(CAMERA_COOLDOWN, ledger.remaining(PHOTO, NOW));

        // The upload failed after all: it tries again in the retry, not the camera's cooldown.
        ledger.afterReport(PHOTO, "failed", RestContext.NONE, NOW + 100);

        assertEquals(RETRY, ledger.remaining(PHOTO, NOW + 100));
    }

    @Test
    void aReportTheSettingsDoNotNameChangesNothing() {
        ledger.afterReport(PHOTO, "stored", RestContext.NONE, NOW);

        ledger.afterReport(PHOTO, "lost_in_the_post", RestContext.NONE, NOW);
        ledger.afterReport(PLANT, "stored", RestContext.NONE, NOW);

        assertEquals(CAMERA_COOLDOWN, ledger.remaining(PHOTO, NOW));
        assertEquals(1, store.writes());
    }

    @Test
    void aRestOnASubjectIsOnThatSubjectAndNotTheErrand() {
        ErrandSettings<String> plant = TestRests.settings(600, new ErrandSettings.Rests(RestSpec.ticks(0),
            RestSpec.ticksPerSubject(200), RestSpec.ticks(0), Map.of()));
        RestLedger plants = ledger(plant);

        plants.afterOutcome(running(PLANT, plant, subject("crop-1")), Outcome.gaveUp("unreachable"), NOW);

        assertTrue(plants.resting(PLANT, "crop-1", NOW));
        assertFalse(plants.resting(PLANT, "crop-2", NOW), "another crop is not rested");
        assertFalse(plants.resting(PLANT, NOW), "nor is the errand as a whole");
        assertEquals(200, plants.remaining(PLANT, "crop-1", NOW));
        assertEquals(0, plants.remaining(PLANT, "crop-2", NOW));
    }

    @Test
    void aRestOnTheErrandAsAWholeIsAlsoARestOnEverySubject() {
        ledger.afterOutcome(running(PHOTO, photo, subject("friend")), Outcome.done(), NOW);

        assertTrue(ledger.resting(PHOTO, "friend", NOW));
        assertTrue(ledger.resting(PHOTO, "anybody", NOW));
    }

    @Test
    void aRestOnASubjectOfAPlanWithNoneIsOnTheErrand() {
        ErrandSettings<String> plant = TestRests.settings(600, new ErrandSettings.Rests(RestSpec.ticks(0),
            RestSpec.ticksPerSubject(200), RestSpec.ticks(0), Map.of()));
        RestLedger plants = ledger(plant);

        plants.afterOutcome(running(PLANT, plant, new Plan() {
        }), Outcome.gaveUp("unreachable"), NOW);

        assertTrue(plants.resting(PLANT, NOW));
    }

    @Test
    void aReportOnASubjectRestsThatSubjectAndNotTheErrand() {
        ErrandSettings<String> scene = scene();
        RestLedger scenes = ledger(scene);

        scenes.afterReport(PHOTO, "played", RestContext.of(subject("pal-1")), NOW);

        assertEquals(SCENE_COOLDOWN, scenes.remaining(PHOTO, "pal-1", NOW));
        assertFalse(scenes.resting(PHOTO, "pal-2", NOW), "another pet is not rested");
        assertFalse(scenes.resting(PHOTO, NOW), "nor is the errand as a whole");
    }

    @Test
    void aReportOnASubjectOfAContextWithNoneRestsTheErrand() {
        RestLedger scenes = ledger(scene());

        scenes.afterReport(PHOTO, "played", RestContext.NONE, NOW);

        assertTrue(scenes.resting(PHOTO, NOW));
    }

    @Test
    void aSceneCutShortAfterItBeganKeepsTheRestItsBeginningMade() {
        ErrandSettings<String> scene = scene();
        RestLedger scenes = ledger(scene);
        scenes.afterReport(PHOTO, "played", RestContext.of(subject("pal-1")), NOW);

        scenes.afterOutcome(running(PHOTO, scene, subject("pal-1")), Outcome.gaveUp("withdrawn"), NOW + 50);
        scenes.afterOutcome(running(PHOTO, scene, subject("pal-1")), Outcome.interrupted("moved"), NOW + 60);

        assertEquals(SCENE_COOLDOWN - 60, scenes.remaining(PHOTO, "pal-1", NOW + 60), "giving up cut the rest short");
    }

    @Test
    void aSceneThatNeverBeganRestsOnlyTheGuardAgainstAskingAgain() {
        ErrandSettings<String> scene = scene();
        RestLedger scenes = ledger(scene);

        scenes.afterOutcome(running(PHOTO, scene, subject("pal-1")), Outcome.gaveUp("declined"), NOW);

        assertEquals(SCENE_GUARD, scenes.remaining(PHOTO, "pal-1", NOW));
        assertFalse(scenes.resting(PHOTO, "pal-1", NOW + SCENE_GUARD));
    }

    @Test
    void aNamedNumberIsAskedOfItsSourceWithWhatTheRestIsAbout() {
        RestSources.register(RestSources.CAMERA_RETRY, context -> context.key().map(key -> key.equals(CAMERA) ? 77L : 5L).orElse(-1L));
        ErrandSettings<String> fromSource = TestRests.settings(1200, new ErrandSettings.Rests(RestSpec.ticks(0),
            RestSpec.from(RestSources.CAMERA_RETRY), RestSpec.ticks(0), Map.of("failed", RestSpec.from(RestSources.CAMERA_RETRY))));
        RestLedger sourced = ledger(fromSource);

        sourced.afterOutcome(running(PHOTO, fromSource, new Plan() {
            @Override
            public Optional<ResourceLocation> restKey() {
                return Optional.of(CAMERA);
            }
        }), Outcome.gaveUp("no"), NOW);
        assertEquals(77, sourced.remaining(PHOTO, NOW));

        sourced.afterReport(PHOTO, "failed", RestContext.of(id("other_camera")), NOW);
        assertEquals(5, sourced.remaining(PHOTO, NOW), "a report says what it is about itself");
    }

    @Test
    void aPresentGivenRestsThePetForTheGiftingCooldownAndNoWayTheErrandEndsAfterwardsCutsItShort() {
        Optional<RestSources.Resolver> registered = RestSources.resolver(RestSources.GIFTING_COOLDOWN);
        RestSources.register(RestSources.GIFTING_COOLDOWN, context -> GIFT_COOLDOWN);
        try {
            presentGiven();
        } finally {
            registered.ifPresent(resolver -> RestSources.register(RestSources.GIFTING_COOLDOWN, resolver));
        }
    }

    private void presentGiven() {
        ErrandSettings<String> gift = giftRests();
        RestLedger gifts = ledger(gift);

        gifts.afterReport(PHOTO, "given", RestContext.NONE, NOW);
        assertEquals(GIFT_COOLDOWN, gifts.remaining(PHOTO, NOW), "the cooldown runs from the giving");

        gifts.afterOutcome(running(PHOTO, gift, new Plan() {
        }), Outcome.done(), NOW + 1);
        gifts.afterOutcome(running(PHOTO, gift, new Plan() {
        }), Outcome.gaveUp("no_gift"), NOW + 2);
        gifts.afterOutcome(running(PHOTO, gift, new Plan() {
        }), Outcome.interrupted("moved"), NOW + 3);

        assertEquals(GIFT_COOLDOWN - 3, gifts.remaining(PHOTO, NOW + 3),
            "done rests for nothing, and giving up for half a minute never takes it down");
    }

    @Test
    void aPresentNeverGivenRestsOnlyAsLongAsTheErrandGaveUpFor() {
        RestLedger gifts = ledger(giftRests());

        gifts.afterOutcome(running(PHOTO, giftRests(), new Plan() {
        }), Outcome.gaveUp("unreachable"), NOW);

        assertEquals(600, gifts.remaining(PHOTO, NOW));
    }

    /** The rests of the gift errand: none for finishing, half a minute for giving up, and the cooldown once a present is given. */
    private static ErrandSettings<String> giftRests() {
        return TestRests.settings(2400, new ErrandSettings.Rests(RestSpec.ticks(0), RestSpec.ticks(600), RestSpec.ticks(0),
            Map.of("given", RestSpec.from(RestSources.GIFTING_COOLDOWN))));
    }

    @Test
    void aNamedNumberThatIsZeroIsTheirChoiceAndNotClamped() {
        RestSources.register(RestSources.CAMERA_COOLDOWN, context -> 0L);
        ErrandSettings<String> zero = TestRests.settings(1200, new ErrandSettings.Rests(RestSpec.ticks(0),
            RestSpec.ticks(1), RestSpec.ticks(0), Map.of("stored", RestSpec.from(RestSources.CAMERA_COOLDOWN))));
        RestLedger sourced = ledger(zero);
        sourced.afterReport(PHOTO, "stored", RestContext.NONE, NOW - 5);
        assertEquals(0, sourced.remaining(PHOTO, NOW), "left at nothing, and said nothing about");

        assertEquals(List.of(), errors);
    }

    @Test
    void aSourceNobodyRegisteredRestsForNothingAndIsSaidToOnce() {
        // Whichever errand registered the music retry by loading the errands, here nobody has.
        Optional<RestSources.Resolver> registered = RestSources.resolver(RestSources.MUSIC_RETRY);
        RestSources.forget(RestSources.MUSIC_RETRY);
        try {
            sourceNobodyRegisteredRestsForNothing();
        } finally {
            registered.ifPresent(resolver -> RestSources.register(RestSources.MUSIC_RETRY, resolver));
        }
    }

    private void sourceNobodyRegisteredRestsForNothing() {
        ErrandSettings<String> missing = TestRests.settings(1200, new ErrandSettings.Rests(RestSpec.ticks(0),
            RestSpec.from(RestSources.MUSIC_RETRY), RestSpec.ticks(0), Map.of()));
        RestLedger sourced = ledger(missing);

        sourced.afterOutcome(running(PHOTO, missing, new Plan() {
        }), Outcome.gaveUp("no"), NOW);
        sourced.afterOutcome(running(PHOTO, missing, new Plan() {
        }), Outcome.gaveUp("no"), NOW);
        ledger(missing).afterOutcome(running(PHOTO, missing, new Plan() {
        }), Outcome.gaveUp("no"), NOW);

        assertEquals(0, sourced.remaining(PHOTO, NOW));
        assertEquals(1, errors.size(), "once, not for every pet and every time");
        assertTrue(errors.get(0).contains(RestSources.MUSIC_RETRY.toString()), errors.get(0));
    }

    /** The rests of a scene: its cooldown from the report that it began, a few seconds if it never did. */
    private static ErrandSettings<String> scene() {
        return TestRests.settings(1200, new ErrandSettings.Rests(RestSpec.ticks(0), RestSpec.ticksPerSubject(SCENE_GUARD),
            RestSpec.ticks(0), Map.of("played", RestSpec.ticksPerSubject(SCENE_COOLDOWN))));
    }

    private RestLedger ledger(ErrandSettings<?> settings) {
        return new RestLedger(errand -> store, errand -> errand.equals(PHOTO) ? Optional.of(settings) : Optional.empty(),
            errors::add);
    }

    private static Running running(ResourceLocation errand, ErrandSettings<?> settings, Plan plan) {
        return new Running(new StubErrand(errand), plan, settings, 0, 0L, Set.of());
    }

    private static Plan subject(String subject) {
        return new Plan() {
            @Override
            public Optional<String> subject() {
                return Optional.of(subject);
            }
        };
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chiikawa", path);
    }
}
