package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.gifting.GiftChoice;
import com.dwinovo.chiikawa.gifting.GiftOutlook;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * When a pet takes a present out of its bag, and which: it has one in mind that it may set off with, its owner is about and
 * may be given one more today, and it has something to give.
 */
class OfferGiftOfferTest {
    private static final GlobalPos AT = GlobalPos.of(ResourceKey.create(ResourceKey.createRegistryKey(
        ResourceLocation.withDefaultNamespace("dimension")), ResourceLocation.withDefaultNamespace("overworld")), BlockPos.ZERO);
    private static final GiftChoice.Pick COOKIE = new GiftChoice.Pick(2, Items.COOKIE, 1);
    private static final GiftChoice.Pick CAKE = new GiftChoice.Pick(4, Items.CAKE, 3);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    private static TestView pet() {
        return TestView.at(AT, new PetAnchor(AT, 0.0, PetAnchor.UNLEASHED, false, true)).ownerPos(AT);
    }

    private static String why(TestView view) {
        return TestErrands.whyNoOffer(ErrandIds.OFFER_GIFT, view).reasonKey();
    }

    @Test
    void aPetWithNothingInMindDoesNotSetOff() {
        assertTrue(TestErrands.offers(ErrandIds.OFFER_GIFT, pet()).isEmpty());
        assertEquals("intent.chiikawa.fail.no_present_in_mind", why(pet()));
    }

    @Test
    void aPetWithAPresentInMindAndSomethingToGiveSetsOff() {
        assertFalse(TestErrands.offers(ErrandIds.OFFER_GIFT, pet().gift(new GiftOutlook(true, 3, List.of(COOKIE)))).isEmpty());
    }

    @Test
    void aPetWithNoOwnerAboutDoesNotSetOff() {
        TestView alone = TestView.at(AT, new PetAnchor(AT, 0.0, PetAnchor.UNLEASHED, false, true))
            .gift(new GiftOutlook(true, 3, List.of(COOKIE)));

        assertEquals("intent.chiikawa.fail.owner_unavailable", why(alone));
    }

    @Test
    void aPetWhoseOwnerHasBeenGivenEnoughTodayDoesNotSetOff() {
        assertEquals("intent.chiikawa.fail.owner_given_enough", why(pet().gift(new GiftOutlook(true, 0, List.of()))));
    }

    @Test
    void aPetWithNothingToGiveDoesNotSetOffAndIsNotOfferedAFillerInItsPlace() {
        assertEquals("intent.chiikawa.fail.no_present", why(pet().gift(new GiftOutlook(true, 3, List.of()))));
    }

    @Test
    void everyThingItMayGiveIsAWayToGoAboutItWeighedByHowMuchThePetFanciesIt() {
        List<? extends Offer<?>> offers = TestErrands.offers(ErrandIds.OFFER_GIFT,
            pet().gift(new GiftOutlook(true, 3, List.of(COOKIE, CAKE))));

        assertEquals(List.of(1, 3), offers.stream().map(Offer::weight).toList());
        assertEquals(new OfferGiftErrand.Offering(CAKE), offers.get(1).plan());
    }

    @Test
    void theChoiceIsTheDecisionsAndTheAnswerIsTheSameEveryTimeItIsAsked() {
        TestView view = pet().gift(new GiftOutlook(true, 3, List.of(COOKIE, CAKE)));

        assertEquals(TestErrands.answer(ErrandIds.OFFER_GIFT, view), TestErrands.answer(ErrandIds.OFFER_GIFT, view));
    }

    @Test
    void thePlanGoesWhereTheOwnerIsSoThatTheAreaItActsInIsAskedAboutThem() {
        TestView view = pet().gift(new GiftOutlook(true, 3, List.of(COOKIE)));

        assertEquals(Optional.of(AT), new OfferGiftErrand.Offering(COOKIE).destination(view));
    }
}
