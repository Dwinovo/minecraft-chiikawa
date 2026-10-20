package com.dwinovo.chiikawa.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestPhotos;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** When a pet may take a photo, and which photos it is offered, as the errand asks. */
class PhotoOutlookTest {
    private static final CameraSettings CAMERA = TestPhotos.CAMERA;
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID FRIEND = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    /** Ready to go: a camera, film, an owner near and calm, and an owner to take a photo of. */
    private static final PhotoOutlook READY = TestPhotos.ready(TestPhotos.of(PhotoSubject.OWNER, OWNER));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static String reason(Check check) {
        assertFalse(check.ok());
        return check.reasonKey().substring("intent.chiikawa.fail.".length());
    }

    private static PhotoOutlook with(boolean hasFilm, boolean ownerInRange, boolean ownerCalm, List<PhotoPlan> moments) {
        return new PhotoOutlook(true, Optional.of(CAMERA), hasFilm, ownerInRange, ownerCalm, moments);
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
        assertEquals("no_camera", reason(new PhotoOutlook(true, Optional.empty(), false, true, true, READY.moments()).canStart()));
        assertEquals("no_film", reason(with(false, true, true, READY.moments()).canStart()));
    }

    @Test
    void itNeedsItsOwnerNearAndInNoStateToBeDisturbed() {
        assertEquals("owner_far", reason(with(true, false, true, READY.moments()).canStart()));
        assertEquals("owner_busy", reason(with(true, true, false, READY.moments()).canStart()));
    }

    @Test
    void withNothingToTakeAPhotoOfItDoesNotStart() {
        assertEquals("nothing_to_shoot", reason(with(true, true, true, List.of()).canStart()));
    }

    @Test
    void aMomentThatPassesWhileItAimsDoesNotPutTheCameraAway() {
        PhotoOutlook momentOver = with(true, true, true, List.of());

        assertTrue(momentOver.canContinue().ok());
        assertFalse(momentOver.canStart().ok());
    }

    @Test
    void theFilmRunningOutOrTheOwnerLeavingStopsAPhotoInTheMiddle() {
        assertEquals("no_film", reason(with(false, true, true, READY.moments()).canContinue()));
        assertEquals("owner_far", reason(with(true, false, true, READY.moments()).canContinue()));
    }

    @Test
    void aKindThePetDoesNotFancyIsNotAMomentItMayChooseHoweverGoodTheMoment() {
        CameraSettings noViews = new CameraSettings(CAMERA.film(), 64.0, 12000, 600, 320, 240, 70,
            new CameraSettings.Subjects(3, 5, 0), 40, 8, 0.5F);
        PhotoOutlook outlook = new PhotoOutlook(true, Optional.of(noViews), true, true, true,
            List.of(TestPhotos.view(), TestPhotos.of(PhotoSubject.FRIEND, FRIEND)));

        assertEquals(List.of(TestPhotos.of(PhotoSubject.FRIEND, FRIEND)), outlook.fancied());
        assertEquals("nothing_to_shoot", reason(new PhotoOutlook(true, Optional.of(noViews), true, true, true,
            List.of(TestPhotos.view())).canStart()));
    }

    @Test
    void theErrandOffersEveryMomentWeightedByWhatThePetFancies() {
        GlobalPos at = GlobalPos.of(ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
            ResourceLocation.withDefaultNamespace("overworld")), BlockPos.ZERO);
        TestView pet = TestView.at(at, new PetAnchor(at, 0.0, PetAnchor.UNLEASHED, false, true));
        TestErrands.load();
        PhotoOutlook all = TestPhotos.ready(TestPhotos.of(PhotoSubject.OWNER, OWNER), TestPhotos.of(PhotoSubject.FRIEND, FRIEND),
            TestPhotos.view());

        // The owner three, a friend five and a view one: the weights the decision draws by.
        assertEquals(List.of(3, 5, 1), TestErrands.offers(ErrandIds.TAKE_PHOTO, pet.photo(all)).stream().map(Offer::weight).toList());
        assertEquals("take_photo", TestErrands.errand(ErrandIds.TAKE_PHOTO).id().getPath());
        assertTrue(TestErrands.offers(ErrandIds.TAKE_PHOTO, pet.photo(PhotoOutlook.NONE)).isEmpty());
        assertFalse(TestErrands.stillWanted(ErrandIds.TAKE_PHOTO, TestErrands.planOf(ErrandIds.TAKE_PHOTO, pet.photo(READY)),
            pet.photo(PhotoOutlook.NONE)).ok());
    }
}
