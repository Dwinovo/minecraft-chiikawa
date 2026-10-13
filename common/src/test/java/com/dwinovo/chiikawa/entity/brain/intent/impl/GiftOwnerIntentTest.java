package com.dwinovo.chiikawa.entity.brain.intent.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.testing.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GiftOwnerIntentTest {
    private static final GlobalPos AT = GlobalPos.of(ResourceKey.create(ResourceKey.createRegistryKey(
        new ResourceLocation("dimension")), new ResourceLocation("overworld")), BlockPos.ZERO);
    private final GiftOwnerIntent intent = new GiftOwnerIntent();

    private static TestContext pet() {
        return TestContext.at(AT, new PetAnchor(AT, 0.0, PetAnchor.UNLEASHED, false, true));
    }

    @Test
    void aPetWithNothingToGiveAndNothingInItsPawsDoesNotSetOff() {
        assertFalse(intent.canRun(pet().build()).ok());
    }

    @Test
    void aPetWithAPresentInMindSetsOffAndOneAlreadyCarryingOneGoesOn() {
        assertTrue(intent.canRun(pet().presentToGive().build()).ok());
        assertTrue(intent.canRun(pet().carryingGift().build()).ok());
    }

    @Test
    void theErrandGoesOnOnlyWhileThePetCarriesThePresent() {
        assertTrue(intent.canContinue(pet().carryingGift().build()).ok());
        // A present in mind is not one in its paws: once it is handed over, or put back, the errand is done.
        assertFalse(intent.canContinue(pet().presentToGive().build()).ok());
    }
}
