package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GiftOwnerOfferTest {
    private static final GlobalPos AT = GlobalPos.of(ResourceKey.create(ResourceKey.createRegistryKey(
        ResourceLocation.withDefaultNamespace("dimension")), ResourceLocation.withDefaultNamespace("overworld")), BlockPos.ZERO);
    private static final GlobalPos BEDSIDE = GlobalPos.of(AT.dimension(), new BlockPos(3, 0, 1));

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    private static TestView pet() {
        return TestView.at(AT, new PetAnchor(AT, 0.0, PetAnchor.UNLEASHED, false, true)).ownerPos(AT);
    }

    @Test
    void aPetCarryingNothingDoesNotSetOffWhateverItHasInMind() {
        assertTrue(TestErrands.offers(ErrandIds.GIFT_OWNER, pet()).isEmpty());
        assertTrue(TestErrands.offers(ErrandIds.GIFT_OWNER, pet().withPresent()).isEmpty(),
            "a present in mind is for taking out of the bag, which is another errand's");
    }

    @Test
    void aPetCarryingAPresentSetsOffToTheOwnerAndStaysOnItAsLongAsItDoes() {
        TestView view = pet().holdingGift();

        assertEquals(new GiftOwnerErrand.Delivery(), TestErrands.planOf(ErrandIds.GIFT_OWNER, view));
        assertTrue(TestErrands.stillWanted(ErrandIds.GIFT_OWNER, new GiftOwnerErrand.Delivery(), view).ok());
        // The present is handed over, or taken from its paws, or put back: the errand is done.
        assertFalse(TestErrands.stillWanted(ErrandIds.GIFT_OWNER, new GiftOwnerErrand.Delivery(), pet()).ok());
    }

    @Test
    void anOwnerAsleepInABedIsGivenTheirPresentByTheBedAndAnAwakeOneIsHandedIt() {
        assertEquals(new GiftOwnerErrand.Bedside(BEDSIDE), TestErrands.planOf(ErrandIds.GIFT_OWNER,
            pet().holdingGift().ownerAsleepBeside(BEDSIDE)));
        assertEquals(new GiftOwnerErrand.Delivery(), TestErrands.planOf(ErrandIds.GIFT_OWNER, pet().holdingGift()));
    }

    @Test
    void theBedsidePlanGoesToTheBedsideSoThatTheAreaItActsInIsAskedAboutIt() {
        TestView view = pet().holdingGift().ownerAsleepBeside(BEDSIDE);

        assertEquals(Optional.of(BEDSIDE), new GiftOwnerErrand.Bedside(BEDSIDE).destination(view));
        assertTrue(TestErrands.stillWanted(ErrandIds.GIFT_OWNER, new GiftOwnerErrand.Bedside(BEDSIDE), view).ok());
    }

    @Test
    void aFightAtTheSamePriorityComesFirst() {
        assertEquals(List.of(WorkData.MELEE, WorkData.RANGED, ErrandIds.DEFEND_SELF),
            ErrandSettings.of(ErrandIds.GIFT_OWNER).orElseThrow().yieldsTo());
    }
}
