package com.dwinovo.chiikawa.entity.brain.errand.decide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestPhotos;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The area a pet acts in is enforced in one place, the admission of an errand, for every errand that
 * goes somewhere: it starts only on a destination within reach and goes on only within the leash.
 */
class AdmissionTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    /** Reach 13, leash 20, round its home. */
    private static final PetAnchor FREE =
        TestAnchors.free(HOME);
    private static final PetTask SLIP = new PetTask(ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding"),
        ResourceLocation.fromNamespaceAndPath("chiikawa", "farmer"), PetWorkCounters.WEED, PetTask.NO_ICON, 8,
        ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("chiikawa", "pet_task/weeding")), 0);
    private static final RestLedger NO_RESTS = new RestLedger(errand -> RestStore.legacy().of(null, errand), errand -> Optional.empty());

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    /** @return the verdict on starting the errand: {@code startable}, or why not */
    private static String verdict(ResourceLocation id, PetView view) {
        DecisionTrace trace = new DecisionTrace();
        ErrandType<?, ?> errand = TestErrands.errand(id);
        ErrandSettings<?> settings = ErrandSettings.of(id).orElseThrow();
        boolean admitted = Admission.of(errand, settings, view, category -> true, NO_RESTS, 0L, trace).isPresent();
        return admitted ? DecisionTrace.STARTABLE : trace.lines().get(0).verdict();
    }

    /** @return how the errand ends for a pet that set out with this plan, or nothing if it goes on */
    private static Optional<Outcome> ending(ResourceLocation id, Plan plan, PetView view) {
        Running running = new Running(id, plan, ErrandSettings.of(id).orElseThrow(), 0, 0L, Set.of());
        return Continuation.endOf(running, TestErrands.errand(id), true, view, category -> true);
    }

    private static TestView shopAt(int blocks) {
        return TestView.at(HOME, FREE).shopWorthVisiting(east(blocks));
    }

    @Test
    void aDestinationWithinReachStarts() {
        assertEquals(DecisionTrace.STARTABLE, verdict(ErrandIds.SHOP, shopAt(10)));
    }

    @Test
    void aDestinationOutsideReachDoesNotStart() {
        assertEquals("intent.chiikawa.fail.out_of_reach", verdict(ErrandIds.SHOP, shopAt(15)));
    }

    @Test
    void aRunningErrandGoesOnWithinTheLeashAndEndsBeyondIt() {
        assertEquals(Optional.empty(), ending(ErrandIds.SHOP, TestErrands.planOf(ErrandIds.SHOP, shopAt(15)), shopAt(15)),
            "a shop a little further is no reason to turn back");
        assertEquals(Optional.of(Outcome.gaveUp("out_of_leash")),
            ending(ErrandIds.SHOP, TestErrands.planOf(ErrandIds.SHOP, shopAt(25)), shopAt(25)));
    }

    @Test
    void anErrandsOwnConditionsComeFirst() {
        assertEquals("intent.chiikawa.fail.has_slip",
            verdict(ErrandIds.TAKE_TASK, TestView.at(HOME, FREE).offeringBoard(east(40)).slip(SLIP)));
    }

    @Test
    void anErrandWithNoDestinationStartsWhereverThePetIs() {
        TestView far = TestView.at(east(500), FREE);

        assertEquals(DecisionTrace.STARTABLE, verdict(ErrandIds.WANDER, far));
    }

    @Test
    void anErrandWithNoDestinationGoesOnOnlyWhileThePetIsWithinTheLeash() {
        TestView near = TestView.at(east(19), FREE).withSong();
        TestView far = TestView.at(east(21), FREE).withSong();

        assertEquals(Optional.empty(), ending(ErrandIds.PLAY_MUSIC, TestErrands.planOf(ErrandIds.PLAY_MUSIC, near), near));
        assertEquals(Optional.of(Outcome.gaveUp("left_area")),
            ending(ErrandIds.PLAY_MUSIC, TestErrands.planOf(ErrandIds.PLAY_MUSIC, near), far));
    }

    @Test
    void aPetWhoseAreaMovesAwayLeavesAnErrandThatStaysWhereItIs() {
        TestView view = TestView.at(HOME, TestAnchors.following(east(500))).withSong();

        assertEquals(Optional.of(Outcome.gaveUp("left_area")),
            ending(ErrandIds.PLAY_MUSIC, TestErrands.planOf(ErrandIds.PLAY_MUSIC, view), view));
    }

    @Test
    void theWayBackToTheAreaIsAnErrandWithADestinationSoItGoesOnToTheEnd() {
        TestView far = TestView.at(east(500), FREE);

        assertEquals(Optional.empty(), ending(ErrandIds.WANDER, returning(far), far));
    }

    private static Plan returning(TestView view) {
        return TestErrands.offers(ErrandIds.WANDER, view).get(0).plan();
    }

    @Test
    void everyErrandSaysWhereItGoes() {
        TestView view = TestView.at(HOME, FREE).ownership(PetOwnership.WILD).phase(DayPhase.NIGHT).withPresent().armedWithArrows()
            .shopWorthVisiting(east(1)).offeringBoard(east(2)).socialPartner(east(3)).examDesk(east(4))
            .resultsDesk(east(5)).whistleHeard(east(6)).ownerPos(east(7))
            .targets(new PerceivedTargets(Optional.of(new AttackTarget(UUID.randomUUID(), east(8))), Optional.of(east(9)), Optional.of(east(10)),
                Optional.of(east(11)), Optional.of(east(12)), Optional.of(east(13)), Optional.of(new PickableItem(UUID.randomUUID(), east(14)))));

        assertEquals(Optional.of(east(1)), destination(ErrandIds.SHOP, view));
        assertEquals(Optional.of(east(2)), destination(ErrandIds.TAKE_TASK, view));
        assertEquals(Optional.of(east(3)), destination(ErrandIds.SOCIALIZE, view));
        assertEquals(Optional.of(east(4)), destination(ErrandIds.TAKE_EXAM, view));
        assertEquals(Optional.of(east(5)), destination(ErrandIds.CHECK_RESULTS, view));
        assertEquals(Optional.of(east(6)), destination(ErrandIds.ANSWER_WHISTLE, view));
        assertEquals(Optional.of(east(7)), destination(ErrandIds.GIFT_OWNER, view));
        assertEquals(Optional.of(east(8)), destination(ErrandIds.MELEE, view));
        assertEquals(Optional.of(east(8)), destination(ErrandIds.RANGED, view));
        assertEquals(Optional.of(east(9)), destination(ErrandIds.HARVEST, view));
        assertEquals(Optional.of(east(10)), destination(ErrandIds.PLANT, view));
        assertEquals(Optional.of(east(11)), destination(ErrandIds.DELIVER, view));
        assertEquals(Optional.of(east(12)), destination(ErrandIds.WEED, view));
        assertEquals(Optional.of(east(13)), destination(ErrandIds.PICK_MUSHROOM, view));
        assertEquals(Optional.of(east(14)), destination(ErrandIds.PICK_UP_ITEM, view));
    }

    @Test
    void errandsThatGoNowhereSayNothing() {
        TestView view = TestView.at(HOME, FREE).askedToPlay().withSong();

        // Pottering about has several ways to go about it, and none of them has anywhere to go.
        for (ResourceLocation habitual : List.of(ErrandIds.WANDER, ErrandIds.STAY)) {
            TestErrands.offers(habitual, view).forEach(offer -> assertEquals(Optional.empty(), offer.plan().destination(view)));
        }
        assertEquals(Optional.empty(), destination(ErrandIds.COOPERATE, view));
        assertEquals(Optional.empty(), destination(ErrandIds.PLAY_MUSIC, view));
    }

    private static Optional<GlobalPos> destination(ResourceLocation id, TestView view) {
        return TestErrands.planOf(id, view).destination(view);
    }

    @Test
    void aPhotoIsWalkedToItsSubjectAndTakenOfAViewWhereThePetStands() {
        assertEquals(Optional.of(east(8)), destination(ErrandIds.TAKE_PHOTO, photoOf(Optional.of(east(8)))));
        assertEquals(Optional.empty(), destination(ErrandIds.TAKE_PHOTO, photoOf(Optional.empty())));
    }

    @Test
    void aPhotoOfSomebodyBeyondTheAreaDoesNotStartAndEndsBeyondTheLeash() {
        assertEquals(DecisionTrace.STARTABLE, verdict(ErrandIds.TAKE_PHOTO, photoOf(Optional.of(east(10)))));
        assertEquals("intent.chiikawa.fail.out_of_reach", verdict(ErrandIds.TAKE_PHOTO, photoOf(Optional.of(east(16)))));
        assertEquals(Optional.empty(), photoEnding(photoOf(Optional.of(east(16)))), "a friend a little further on");
        assertEquals(Optional.of(Outcome.gaveUp("out_of_leash")), photoEnding(photoOf(Optional.of(east(25)))));
        assertEquals(DecisionTrace.STARTABLE, verdict(ErrandIds.TAKE_PHOTO, photoOf(Optional.empty())), "a view is taken where it stands");
    }

    private static Optional<Outcome> photoEnding(TestView view) {
        return ending(ErrandIds.TAKE_PHOTO, TestErrands.planOf(ErrandIds.TAKE_PHOTO, view), view);
    }

    private static final UUID FRIEND = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    /** A photo of a friend where it is, or of a view when there is none. */
    private static TestView photoOf(Optional<GlobalPos> where) {
        TestView view = TestView.at(HOME, FREE);
        return where.map(found -> view.creature(FRIEND, found).photo(TestPhotos.ready(TestPhotos.of(PhotoSubject.FRIEND, FRIEND))))
            .orElseGet(() -> view.photo(TestPhotos.ready(TestPhotos.view())));
    }

    @Test
    void aPetInItsChairHasNowhereLeftToGo() {
        TestView seated = TestView.at(HOME, FREE).examDesk(east(40)).seated();

        assertEquals(Optional.empty(), destination(ErrandIds.TAKE_EXAM, seated));
        assertEquals(Optional.empty(), ending(ErrandIds.TAKE_EXAM, TestErrands.planOf(ErrandIds.TAKE_EXAM, seated), seated),
            "the desk it sits at is not what the leash measures once it is in the chair");
    }

    @Test
    void aFollowingPetsOwnerIsTheMiddleOfItsAreaSoItIsAlwaysAdmitted() {
        GlobalPos owner = east(500);
        TestView view = TestView.at(HOME, TestAnchors.following(owner));

        assertEquals(Optional.of(owner), destination(ErrandIds.FOLLOW_OWNER, view));
        assertEquals(DecisionTrace.STARTABLE, verdict(ErrandIds.FOLLOW_OWNER, view));
    }

    @Test
    void aPresentIsOnlyWalkedToAnOwnerWithinTheArea() {
        assertEquals(DecisionTrace.STARTABLE, verdict(ErrandIds.GIFT_OWNER, TestView.at(HOME, FREE).withPresent().ownerPos(east(10))));
        assertEquals("intent.chiikawa.fail.out_of_reach",
            verdict(ErrandIds.GIFT_OWNER, TestView.at(HOME, FREE).withPresent().ownerPos(east(30))));
        TestView carrying = TestView.at(HOME, FREE).holdingGift().ownerPos(east(30));
        assertEquals(Optional.of(Outcome.gaveUp("out_of_leash")),
            ending(ErrandIds.GIFT_OWNER, TestErrands.planOf(ErrandIds.GIFT_OWNER, carrying), carrying));
    }

    @Test
    void anErrandThatIsNoLongerOfferedOrPermittedEndsAsTheCapabilityOrTheDirectiveChanged() {
        TestView view = shopAt(5);
        Running running = new Running(ErrandIds.SHOP, TestErrands.planOf(ErrandIds.SHOP, view),
            ErrandSettings.of(ErrandIds.SHOP).orElseThrow(), 0, 0L, Set.of());
        ErrandType<?, ?> shop = TestErrands.errand(ErrandIds.SHOP);

        assertEquals(Optional.of(Outcome.interrupted("capability")), Continuation.endOf(running, shop, false, view, category -> true));
        assertEquals(Optional.of(Outcome.interrupted("directive")), Continuation.endOf(running, shop, true, view, category -> false));
        assertTrue(Continuation.endOf(running, shop, true, view, category -> true).isEmpty());
    }
}
