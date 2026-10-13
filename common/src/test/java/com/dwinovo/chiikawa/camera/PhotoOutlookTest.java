package com.dwinovo.chiikawa.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.impl.TakePhotoIntent;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.testing.TestContext;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** When a pet may take a photo, as the intent asks it. */
class PhotoOutlookTest {
    private static final CameraSettings CAMERA = new CameraSettings(new ResourceLocation("chiikawa", "film"),
        64.0, 12000, 600, 320, 240, 70, new CameraSettings.Subjects(3, 5, 1), 40, 8, 0.5F);
    /** Ready to go: a camera, film, an owner near and calm, and an owner to take a photo of. */
    private static final PhotoOutlook READY = new PhotoOutlook(true, Optional.of(CAMERA), true, true, true, false,
        Optional.of(new PhotoPlan(PhotoSubject.OWNER, Optional.of(UUID.randomUUID()), "Owner", Vec3.ZERO, Optional.empty())));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static String reason(IntentCheck check) {
        assertFalse(check.ok());
        return check.reasonKey().substring("intent.chiikawa.fail.".length());
    }

    @Test
    void aPetWithEverythingAndAGoodMomentTakesAPhoto() {
        assertTrue(READY.canStart().ok());
        assertTrue(READY.canContinue().ok());
    }

    @Test
    void aPetThatTakesNoPhotosNeverStarts() {
        assertEquals("not_photographer", reason(PhotoOutlook.NONE.canStart()));
    }

    @Test
    void itNeedsACameraAndFilmForIt() {
        assertEquals("no_camera", reason(new PhotoOutlook(true, Optional.empty(), false, true, true, false,
            READY.plan()).canStart()));
        assertEquals("no_film", reason(new PhotoOutlook(true, Optional.of(CAMERA), false, true, true, false,
            READY.plan()).canStart()));
    }

    @Test
    void itNeedsItsOwnerNearAndInNoStateToBeDisturbed() {
        assertEquals("owner_far", reason(new PhotoOutlook(true, Optional.of(CAMERA), true, false, true, false,
            READY.plan()).canStart()));
        assertEquals("owner_busy", reason(new PhotoOutlook(true, Optional.of(CAMERA), true, true, false, false,
            READY.plan()).canStart()));
    }

    @Test
    void itRestsAfterAPhotoAndWithNothingToTakeOneOfItDoesNotStart() {
        assertEquals("photo_resting", reason(new PhotoOutlook(true, Optional.of(CAMERA), true, true, true, true,
            READY.plan()).canStart()));
        assertEquals("nothing_to_shoot", reason(new PhotoOutlook(true, Optional.of(CAMERA), true, true, true, false,
            Optional.empty()).canStart()));
    }

    @Test
    void aMomentThatPassesWhileItAimsDoesNotPutTheCameraAway() {
        PhotoOutlook momentOver = new PhotoOutlook(true, Optional.of(CAMERA), true, true, true, false, Optional.empty());

        assertTrue(momentOver.canContinue().ok());
        assertFalse(momentOver.canStart().ok());
    }

    @Test
    void theRestAfterTheShutterEndsTheIntentSoThePetIsNotLeftHoldingTheCameraUp() {
        PhotoOutlook shot = new PhotoOutlook(true, Optional.of(CAMERA), true, true, true, true, READY.plan());

        assertEquals("photo_resting", reason(shot.canContinue()));
    }

    @Test
    void theFilmRunningOutOrTheOwnerLeavingStopsAPhotoInTheMiddle() {
        assertEquals("no_film", reason(new PhotoOutlook(true, Optional.of(CAMERA), false, true, true, false,
            READY.plan()).canContinue()));
        assertEquals("owner_far", reason(new PhotoOutlook(true, Optional.of(CAMERA), true, false, true, false,
            READY.plan()).canContinue()));
    }

    @Test
    void theIntentAsksTheOutlook() {
        TakePhotoIntent intent = new TakePhotoIntent();
        GlobalPos at = GlobalPos.of(ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation("dimension")),
            new ResourceLocation("overworld")), BlockPos.ZERO);
        TestContext pet = TestContext.at(at, new PetAnchor(at, 0.0, PetAnchor.UNLEASHED, false, true));

        assertTrue(intent.canRun(pet.photo(READY).build()).ok());
        assertEquals("take_photo", intent.id().getPath());
        assertFalse(intent.canRun(pet.photo(PhotoOutlook.NONE).build()).ok());
        assertFalse(intent.canContinue(pet.photo(PhotoOutlook.NONE).build()).ok());
    }
}
