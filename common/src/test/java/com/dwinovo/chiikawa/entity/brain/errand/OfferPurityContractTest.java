package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * An errand is asked what it offers by a pet that is only being looked at, so what it answers is the
 * pet's and the errand's alone: the same twice, and the pet as it was. (That it draws nothing from a
 * random cannot be seen here, for the view has none to draw from; the game tests see it on a live pet.)
 */
class OfferPurityContractTest {
    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void everyErrandAnswersTheSameTwiceAndLeavesThePetAsItWas() {
        Errands.all().forEach(errand -> {
            TestView view = ErrandFixtures.of(errand.id()).orElseThrow(() -> new AssertionError(errand.id() + " has no fixture"));
            List<?> before = snapshot(view);

            Offers<?> first = TestErrands.answer(errand.id(), view);
            Offers<?> second = TestErrands.answer(errand.id(), view);

            assertFalse(first.isEmpty(), errand.id().toString());
            assertEquals(first, second, errand.id().toString());
            assertEquals(before, snapshot(view), errand.id() + " changed the pet it was asked about");
        });
    }

    @Test
    void theReasonAnErrandOffersNothingIsPureToo() {
        Errands.all().forEach(errand -> {
            // The fixture of any other errand is a pet it has nothing to offer, most of the time.
            Errands.all().stream().filter(other -> other != errand).forEach(other -> {
                TestView elsewhere = ErrandFixtures.of(other.id()).orElseThrow();
                List<?> before = snapshot(elsewhere);

                assertEquals(TestErrands.answer(errand.id(), elsewhere), TestErrands.answer(errand.id(), elsewhere));
                assertEquals(before, snapshot(elsewhere));
            });
        });
    }

    /** Everything an errand can learn of the pet, as the view answers it. */
    private static List<?> snapshot(PetView view) {
        return List.of(view.pos(), view.phase(), view.ownership(), view.personality(), view.anchor(), view.follow(),
            view.targets(), view.slip(), view.ownerPos(), view.eager(), view.offeringBoard(), view.shopWorthVisiting(),
            view.socialPartner(), view.playingAlong(),
            view.carryingGift(), view.presentToGive(), view.attackCoolingDown(), view.hasArrows(),
            view.playableSelection(), view.examToSit(), view.resultsDesk(), view.seatedAtExam(),
            view.whistleHeard(), view.photo());
    }
}
