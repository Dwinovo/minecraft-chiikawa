package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FollowOwnerOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos OWNER = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FOLLOWING = TestAnchors.following(OWNER);

    private final ErrandSettings<?> settings = ErrandSettingsData.all().get(ErrandIds.FOLLOW_OWNER);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void scoreRisesFromHalfAtFollowStartToOneAtReach() {
        assertEquals(0.5F, score(petAt(7, FOLLOWING)), 1.0E-6F);
        assertEquals(0.6F, score(petAt(8, FOLLOWING)), 1.0E-6F);
        assertEquals(0.8F, score(petAt(10, FOLLOWING)), 1.0E-6F);
        assertEquals(1.0F, score(petAt(12, FOLLOWING)), 1.0E-6F);
    }

    @Test
    void scoreIsClampedOutsideTheFollowRange() {
        assertEquals(0.5F, score(petAt(3, FOLLOWING)), 1.0E-6F);
        assertEquals(1.0F, score(petAt(19, FOLLOWING)), 1.0E-6F);
    }

    @Test
    void aFarOwnerOutscoresEveryOtherErrandOfFollowingsPriority() {
        float far = score(petAt(12, FOLLOWING));

        ErrandSettingsData.all().forEach((errand, other) -> {
            if (!errand.equals(ErrandIds.FOLLOW_OWNER) && other.priority() == settings.priority()) {
                assertTrue(far > other.score(), errand.toString());
            }
        });
    }

    @Test
    void startsAtFollowStartAndContinuesUntilArrived() {
        assertTrue(TestErrands.offers(ErrandIds.FOLLOW_OWNER, petAt(6, FOLLOWING)).isEmpty());
        assertEquals(1, TestErrands.offers(ErrandIds.FOLLOW_OWNER, petAt(7, FOLLOWING)).size());

        assertTrue(TestErrands.stillWanted(ErrandIds.FOLLOW_OWNER, Heading.NOWHERE, petAt(3, FOLLOWING)).ok());
        assertFalse(TestErrands.stillWanted(ErrandIds.FOLLOW_OWNER, Heading.NOWHERE,
            petAt(TestAnchors.SETTINGS.follow().arrive(), FOLLOWING)).ok());
    }

    @Test
    void neverStartsWithoutAnAnchorThatFollowsTheOwner() {
        GlobalPos far = GlobalPos.of(OVERWORLD, new BlockPos(30, 64, 0));
        PetAnchor offline = new PetAnchor(far, 0.0, PetAnchor.UNLEASHED, false, true);
        TestView view = TestView.at(far, offline);

        assertTrue(TestErrands.offers(ErrandIds.FOLLOW_OWNER, view).isEmpty());
        assertEquals("intent.chiikawa.fail.owner_unavailable", TestErrands.whyNoOffer(ErrandIds.FOLLOW_OWNER, view).reasonKey());
        assertFalse(TestErrands.stillWanted(ErrandIds.FOLLOW_OWNER, Heading.NOWHERE, view).ok());
    }

    @Test
    void itHeadsForTheOwnerWhoIsTheMiddleOfItsArea() {
        TestView view = petAt(10, FOLLOWING);

        assertEquals(Optional.of(OWNER), TestErrands.planOf(ErrandIds.FOLLOW_OWNER, view).destination(view));
    }

    private static float score(TestView view) {
        return TestErrands.score(ErrandIds.FOLLOW_OWNER, view);
    }

    private static TestView petAt(int blocksEast, PetAnchor anchor) {
        GlobalPos pet = GlobalPos.of(OVERWORLD, OWNER.pos().east(blocksEast));
        return TestView.at(pet, anchor);
    }
}
