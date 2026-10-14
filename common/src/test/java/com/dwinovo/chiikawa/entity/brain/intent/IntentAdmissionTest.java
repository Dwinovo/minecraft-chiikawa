package com.dwinovo.chiikawa.entity.brain.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.camera.PhotoPlan;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector.Candidate;
import com.dwinovo.chiikawa.testing.TestContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * The area a pet acts in is enforced in one place, the selector, for every intent that goes
 * somewhere: it starts only on a destination within reach and goes on only within the leash.
 */
class IntentAdmissionTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
        Identifier.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    /** Reach 13, leash 20, round its home. */
    private static final PetAnchor FREE =
        TestAnchors.free(HOME);

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    private static Candidate only(PetIntent intent, IntentContext ctx, boolean running) {
        return IntentSelector.candidates(List.of(intent), ctx, running ? intent.id() : null, category -> true).get(0);
    }

    private static IntentContext shopAt(int blocks) {
        return TestContext.at(HOME, FREE).shopWorthVisiting(east(blocks)).build();
    }

    @Test
    void aDestinationWithinReachStarts() {
        assertTrue(only(PetIntents.get(PetIntents.SHOP), shopAt(10), false).eligible());
    }

    @Test
    void aDestinationOutsideReachDoesNotStart() {
        Candidate shop = only(PetIntents.get(PetIntents.SHOP), shopAt(15), false);

        assertEquals("intent.chiikawa.fail.out_of_reach", shop.check().reasonKey());
    }

    @Test
    void aRunningIntentGoesOnWithinTheLeashAndEndsBeyondIt() {
        PetIntent shop = PetIntents.get(PetIntents.SHOP);

        assertTrue(only(shop, shopAt(15), true).eligible(), "a shop a little further is no reason to turn back");
        assertEquals("intent.chiikawa.fail.out_of_leash", only(shop, shopAt(25), true).check().reasonKey());
    }

    @Test
    void anIntentsOwnConditionsComeFirst() {
        Candidate shop = only(PetIntents.get(PetIntents.SHOP),
            TestContext.at(HOME, FREE).shopWorthVisiting(east(40)).shopCoolingDown().build(), false);

        assertEquals("intent.chiikawa.fail.shop_resting", shop.check().reasonKey());
    }

    @Test
    void anIntentWithNoDestinationIsUnaffected() {
        IntentContext far = TestContext.at(east(500), FREE).build();

        assertTrue(only(PetIntents.get(PetIntents.WANDER), far, false).eligible());
        assertTrue(only(PetIntents.get(PetIntents.WANDER), far, true).eligible());
    }

    @Test
    void everyErrandSaysWhereItGoes() {
        IntentContext ctx = TestContext.at(HOME, FREE)
            .shopWorthVisiting(east(1)).offeringBoard(east(2)).socialPartner(east(3)).examDesk(east(4))
            .resultsDesk(east(5)).whistleHeard(east(6)).ownerPos(east(7))
            .targets(new PerceivedTargets(Optional.of(east(8)), Optional.of(east(9)), Optional.of(east(10)),
                Optional.of(east(11)), Optional.of(east(12)), Optional.of(east(13)), Optional.of(east(14))))
            .build();

        assertEquals(Optional.of(east(1)), PetIntents.get(PetIntents.SHOP).destination(ctx));
        assertEquals(Optional.of(east(2)), PetIntents.get(PetIntents.TAKE_TASK).destination(ctx));
        assertEquals(Optional.of(east(3)), PetIntents.get(PetIntents.SOCIALIZE).destination(ctx));
        assertEquals(Optional.of(east(4)), PetIntents.get(PetIntents.TAKE_EXAM).destination(ctx));
        assertEquals(Optional.of(east(5)), PetIntents.get(PetIntents.CHECK_RESULTS).destination(ctx));
        assertEquals(Optional.of(east(6)), PetIntents.get(PetIntents.ANSWER_WHISTLE).destination(ctx));
        assertEquals(Optional.of(east(7)), PetIntents.get(PetIntents.GIFT_OWNER).destination(ctx));
        assertEquals(Optional.of(east(8)), PetIntents.get(PetIntents.MELEE).destination(ctx));
        assertEquals(Optional.of(east(8)), PetIntents.get(PetIntents.RANGED).destination(ctx));
        assertEquals(Optional.of(east(9)), PetIntents.get(PetIntents.HARVEST).destination(ctx));
        assertEquals(Optional.of(east(10)), PetIntents.get(PetIntents.PLANT).destination(ctx));
        assertEquals(Optional.of(east(11)), PetIntents.get(PetIntents.DELIVER).destination(ctx));
        assertEquals(Optional.of(east(12)), PetIntents.get(PetIntents.WEED).destination(ctx));
        assertEquals(Optional.of(east(13)), PetIntents.get(PetIntents.PICK_MUSHROOM).destination(ctx));
        assertEquals(Optional.of(east(14)), PetIntents.get(PetIntents.PICK_UP_ITEM).destination(ctx));
    }

    @Test
    void errandsThatGoNowhereSayNothing() {
        IntentContext ctx = TestContext.at(HOME, FREE).playingAlong().build();

        assertEquals(Optional.empty(), PetIntents.get(PetIntents.WANDER).destination(ctx));
        assertEquals(Optional.empty(), PetIntents.get(PetIntents.STAY).destination(ctx));
        assertEquals(Optional.empty(), PetIntents.get(PetIntents.COOPERATE).destination(ctx));
        assertEquals(Optional.empty(), PetIntents.get(PetIntents.PLAY_MUSIC).destination(ctx));
        assertEquals(Optional.empty(), PetIntents.get(PetIntents.TAKE_PHOTO).destination(ctx));
    }

    @Test
    void aPhotoIsWalkedToItsSubjectAndTakenOfAViewWhereThePetStands() {
        PetIntent photo = PetIntents.get(PetIntents.TAKE_PHOTO);

        assertEquals(Optional.of(east(8)), photo.destination(photoOf(Optional.of(east(8))).build()));
        assertEquals(Optional.empty(), photo.destination(photoOf(Optional.empty()).build()));
    }

    @Test
    void aPhotoOfSomebodyBeyondTheAreaDoesNotStartAndEndsBeyondTheLeash() {
        PetIntent photo = PetIntents.get(PetIntents.TAKE_PHOTO);

        assertTrue(only(photo, photoOf(Optional.of(east(10))).build(), false).eligible());
        assertEquals("intent.chiikawa.fail.out_of_reach", only(photo, photoOf(Optional.of(east(16))).build(), false).check().reasonKey());
        assertTrue(only(photo, photoOf(Optional.of(east(16))).build(), true).eligible(), "a friend a little further on");
        assertEquals("intent.chiikawa.fail.out_of_leash", only(photo, photoOf(Optional.of(east(25))).build(), true).check().reasonKey());
        assertTrue(only(photo, photoOf(Optional.empty()).build(), false).eligible(), "a view is taken where it stands");
    }

    private static TestContext photoOf(Optional<GlobalPos> where) {
        PhotoOutlook outlook = new PhotoOutlook(true, Optional.of(new CameraSettings(
            Identifier.fromNamespaceAndPath("chiikawa", "film"), 64.0, 12000, 600, 320, 240, 70,
            new CameraSettings.Subjects(1, 1, 1), 40, 8, 0.5F)), true, true, true, false,
            Optional.of(new PhotoPlan(PhotoSubject.FRIEND, Optional.empty(), "", Vec3.ZERO, where)));
        return TestContext.at(HOME, FREE).photo(outlook);
    }

    @Test
    void aPetInItsChairHasNowhereLeftToGo() {
        IntentContext seated = TestContext.at(HOME, FREE).examDesk(east(40)).seatedAtExam().build();
        PetIntent exam = PetIntents.get(PetIntents.TAKE_EXAM);

        assertEquals(Optional.empty(), exam.destination(seated));
        assertTrue(only(exam, seated, true).eligible(), "the leash does not take it out of its chair");
    }

    @Test
    void aFollowingPetsOwnerIsTheMiddleOfItsAreaSoItIsAlwaysAdmitted() {
        GlobalPos owner = east(500);
        PetAnchor following = TestAnchors.following(owner);
        IntentContext ctx = TestContext.at(HOME, following).build();
        PetIntent follow = PetIntents.get(PetIntents.FOLLOW_OWNER);

        assertEquals(Optional.of(owner), follow.destination(ctx));
        assertTrue(only(follow, ctx, false).eligible());
    }

    @Test
    void aPresentIsOnlyWalkedToAnOwnerWithinTheArea() {
        PetIntent gift = PetIntents.get(PetIntents.GIFT_OWNER);

        assertTrue(only(gift, TestContext.at(HOME, FREE).presentToGive().ownerPos(east(10)).build(), false).eligible());
        assertEquals("intent.chiikawa.fail.out_of_reach",
            only(gift, TestContext.at(HOME, FREE).presentToGive().ownerPos(east(30)).build(), false).check().reasonKey());
        assertEquals("intent.chiikawa.fail.out_of_leash",
            only(gift, TestContext.at(HOME, FREE).carryingGift().ownerPos(east(30)).build(), true).check().reasonKey());
    }
}
