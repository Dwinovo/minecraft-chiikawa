package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GiftOwnerOfferTest {
    private static final GlobalPos AT = GlobalPos.of(ResourceKey.create(ResourceKey.createRegistryKey(
        ResourceLocation.withDefaultNamespace("dimension")), ResourceLocation.withDefaultNamespace("overworld")), BlockPos.ZERO);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    private static TestView pet() {
        return TestView.at(AT, new PetAnchor(AT, 0.0, PetAnchor.UNLEASHED, false, true));
    }

    @Test
    void aPetWithNothingToGiveAndNothingInItsPawsDoesNotSetOff() {
        assertTrue(TestErrands.offers(ErrandIds.GIFT_OWNER, pet()).isEmpty());
    }

    @Test
    void aPetWithAPresentInMindSetsOffAndOneAlreadyCarryingOneGoesOn() {
        assertFalse(TestErrands.offers(ErrandIds.GIFT_OWNER, pet().withPresent()).isEmpty());
        assertFalse(TestErrands.offers(ErrandIds.GIFT_OWNER, pet().holdingGift()).isEmpty());
    }

    @Test
    void theErrandGoesOnOnlyWhileThePetCarriesThePresent() {
        assertTrue(TestErrands.stillWanted(ErrandIds.GIFT_OWNER, Heading.NOWHERE, pet().holdingGift()).ok());
        // A present in mind is not one in its paws: once it is handed over, or put back, the errand is done.
        assertFalse(TestErrands.stillWanted(ErrandIds.GIFT_OWNER, Heading.NOWHERE, pet().withPresent()).ok());
    }
}
