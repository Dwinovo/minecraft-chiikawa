package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.PhotoResults;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestContext;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestPhotos;
import com.dwinovo.chiikawa.testing.TestRests;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * How long Hachiware rests from photos, with the numbers the shipped settings and the camera give: the
 * ordering of the rest an errand that ends writes and the rest a report writes. The shutter reports the
 * wait for the upload, so that however the errand ends after it, it has rested for it, and one that ends
 * before it has not; a photo stored while the camera is still up is not cut short by what follows, and one
 * that did not come back waits only for the retry.
 */
class TakePhotoRestsTest {
    private static final ResourceLocation PHOTO = ErrandIds.TAKE_PHOTO;
    private static final long NOW = 5000L;
    private static final CameraSettings CAMERA = TestPhotos.CAMERA;

    private final TestRests.Store store = new TestRests.Store();
    private RestLedger ledger;
    private Running photo;
    private Map<ResourceLocation, CameraSettings> inForce;

    @BeforeEach
    void setUp() {
        TestErrands.load();
        // Loads the errands, which is what registers the numbers its rests point at.
        Errands.get(PHOTO);
        inForce = CameraSettings.all();
        CameraSettings.replaceAll(Map.of(TestPhotos.CAMERA_ITEM, CAMERA));
        ledger = new RestLedger(errand -> store, ErrandSettings::of);
        photo = new Running(PHOTO, new TakePhotoErrand.Shot(TestPhotos.view()), ErrandSettings.of(PHOTO).orElseThrow(), 0, 0L, Set.of());
    }

    @AfterEach
    void tearDown() {
        CameraSettings.replaceAll(inForce);
    }

    private void report(String report, long now) {
        ledger.afterReport(PHOTO, report, RestContext.of(TestPhotos.CAMERA_ITEM), now);
    }

    @Test
    void theShutterRestsThePetForThePictureFromThenOn() {
        report(PhotoResults.TAKEN, NOW);

        assertEquals(PhotoUploads.WAIT_TICKS, ledger.remaining(PHOTO, NOW));
        assertEquals(PhotoUploads.WAIT_TICKS - 15, ledger.remaining(PHOTO, NOW + 15));
    }

    @Test
    void aPetCutShortAfterTheShutterStillWaitsForThePictureAndOneCutShortBeforeItRestsNotAtAll() {
        report(PhotoResults.TAKEN, NOW);
        ledger.afterOutcome(photo, Outcome.interrupted("moved"), NOW + 1);
        assertEquals(PhotoUploads.WAIT_TICKS - 1, ledger.remaining(PHOTO, NOW + 1));

        TestRests.Store before = new TestRests.Store();
        new RestLedger(errand -> before, ErrandSettings::of).afterOutcome(photo, Outcome.interrupted("higher"), NOW);
        assertEquals(0, before.remaining(null, NOW));
    }

    @Test
    void anErrandThatEndsAfterTheShutterDoesNotCutTheWaitShortAndDoneAddsNothingOfItsOwn() {
        report(PhotoResults.TAKEN, NOW);
        ledger.afterOutcome(photo, Outcome.done(), NOW + 20);
        assertEquals(PhotoUploads.WAIT_TICKS - 20, ledger.remaining(PHOTO, NOW + 20));
        ledger.afterOutcome(photo, Outcome.gaveUp("not_coming"), NOW + 20);
        assertEquals(PhotoUploads.WAIT_TICKS - 20, ledger.remaining(PHOTO, NOW + 20));

        TestRests.Store never = new TestRests.Store();
        new RestLedger(errand -> never, ErrandSettings::of).afterOutcome(photo, Outcome.done(), NOW);
        assertEquals(0, never.remaining(null, NOW), "done is the shutter's, it rests for nothing of its own");
    }

    @Test
    void aPetThatGaveUpBeforeTheShutterWaitsOnlyForTheRetry() {
        ledger.afterOutcome(photo, Outcome.gaveUp("unreachable"), NOW);

        assertEquals(CAMERA.retryTicks(), ledger.remaining(PHOTO, NOW));
    }

    @Test
    void aPhotoStoredWhileTheCameraIsStillUpKeepsTheCamerasWholeRestThroughTheWaitThatFollows() {
        report(PhotoResults.STORED, NOW);
        assertEquals(CAMERA.cooldownTicks(), ledger.remaining(PHOTO, NOW));

        // The pose is held a little longer, then the errand is done: the wait for the upload does not cut it short.
        ledger.afterOutcome(photo, Outcome.done(), NOW + 15);
        assertEquals(CAMERA.cooldownTicks() - 15, ledger.remaining(PHOTO, NOW + 15));
    }

    @Test
    void aPhotoStoredAfterTheErrandIsDoneReplacesTheWaitWithTheCamerasRest() {
        report(PhotoResults.TAKEN, NOW);
        ledger.afterOutcome(photo, Outcome.done(), NOW + 20);
        report(PhotoResults.STORED, NOW + 100);

        assertEquals(CAMERA.cooldownTicks(), ledger.remaining(PHOTO, NOW + 100));
    }

    @Test
    void aPhotoThatDoesNotComeBackShortensTheWaitToTheRetry() {
        report(PhotoResults.TAKEN, NOW);
        ledger.afterOutcome(photo, Outcome.done(), NOW + 20);
        report(PhotoResults.FAILED, NOW + 100);

        assertEquals(CAMERA.retryTicks(), ledger.remaining(PHOTO, NOW + 100));
    }

    @Test
    void theNumbersAreTheCamerasOfTheItemThePhotoWasTakenWith() {
        ResourceLocation other = ResourceLocation.fromNamespaceAndPath("chiikawa", "better_camera");
        CameraSettings better = new CameraSettings(CAMERA.film(), 64.0, 24000, 60, 320, 240, 70, CAMERA.subjects(), 40, 8, 0.5F);
        CameraSettings.replaceAll(Map.of(TestPhotos.CAMERA_ITEM, CAMERA, other, better));

        ledger.afterReport(PHOTO, PhotoResults.STORED, RestContext.of(other), NOW);
        assertEquals(24000, ledger.remaining(PHOTO, NOW));
        ledger.afterReport(PHOTO, PhotoResults.FAILED, RestContext.of(other), NOW);
        assertEquals(60, ledger.remaining(PHOTO, NOW));
    }

    @Test
    void aCameraNoPackMakesAnyMoreRestsForNothingRatherThanForAGuess() {
        ledger.afterReport(PHOTO, PhotoResults.STORED, RestContext.of(ResourceLocation.fromNamespaceAndPath("chiikawa", "gone")), NOW);

        assertEquals(0, ledger.remaining(PHOTO, NOW));
    }
}
