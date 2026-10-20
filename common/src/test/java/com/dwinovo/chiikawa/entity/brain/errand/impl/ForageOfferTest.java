package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ForageOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor WILD_ANCHOR = TestAnchors.wild(HOME);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void foragingIsItsOwnCategoryAndReportsItsWork() {
        assertEquals(ErrandCategory.FORAGE, TestErrands.errand(ErrandIds.WEED).category());
        assertEquals(ErrandCategory.FORAGE, TestErrands.errand(ErrandIds.PICK_MUSHROOM).category());
        assertEquals(Optional.of(PetWorkCounters.WEED), TestErrands.errand(ErrandIds.WEED).workCounter());
        assertEquals(Optional.of(PetWorkCounters.PICK_MUSHROOM), TestErrands.errand(ErrandIds.PICK_MUSHROOM).workCounter());
    }

    @Test
    void wildFarmerPullsWeedsAtAnyTimeAndTheMindKeepsItWithinReach() {
        for (DayPhase phase : DayPhase.values()) {
            assertFalse(TestErrands.offers(ErrandIds.WEED, view(PetOwnership.WILD, phase, 20)).isEmpty(), phase::name);
        }
        TestView far = view(PetOwnership.WILD, DayPhase.DAY, 24);
        assertEquals(Optional.of(GlobalPos.of(OVERWORLD, HOME.pos().east(24))),
            TestErrands.planOf(ErrandIds.WEED, far).destination(far));
        assertEquals("intent.chiikawa.fail.no_weed",
            TestErrands.whyNoOffer(ErrandIds.WEED, TestView.at(HOME, WILD_ANCHOR).ownership(PetOwnership.WILD)).reasonKey());
    }

    @Test
    void ownedFarmerForagesOnlyWithAMatchingSlip() {
        assertEquals("intent.chiikawa.fail.needs_slip",
            TestErrands.whyNoOffer(ErrandIds.WEED, view(TestView.OWNED, DayPhase.DAY, 1)).reasonKey());
        assertEquals("intent.chiikawa.fail.needs_slip",
            TestErrands.whyNoOffer(ErrandIds.PICK_MUSHROOM, view(TestView.OWNED, DayPhase.NIGHT, 1)).reasonKey());

        assertFalse(TestErrands.offers(ErrandIds.WEED, view(TestView.OWNED, DayPhase.DAY, 1).slip(slip(PetWorkCounters.WEED))).isEmpty());
        assertFalse(TestErrands.offers(ErrandIds.PICK_MUSHROOM,
            view(TestView.OWNED, DayPhase.NIGHT, 1).slip(slip(PetWorkCounters.PICK_MUSHROOM))).isEmpty());
        // A weeding slip does not send it after mushrooms.
        assertEquals("intent.chiikawa.fail.needs_slip", TestErrands.whyNoOffer(ErrandIds.PICK_MUSHROOM,
            view(TestView.OWNED, DayPhase.NIGHT, 1).slip(slip(PetWorkCounters.WEED))).reasonKey());
    }

    @Test
    void mushroomsArePickedOnlyAtNight() {
        assertFalse(TestErrands.offers(ErrandIds.PICK_MUSHROOM, view(PetOwnership.WILD, DayPhase.NIGHT, 1)).isEmpty());
        for (DayPhase phase : new DayPhase[] {DayPhase.MORNING, DayPhase.DAY, DayPhase.EVENING}) {
            assertEquals("intent.chiikawa.fail.not_night",
                TestErrands.whyNoOffer(ErrandIds.PICK_MUSHROOM, view(PetOwnership.WILD, phase, 1)).reasonKey());
        }
        assertTrue(TestErrands.stillWanted(ErrandIds.PICK_MUSHROOM, workAt(1), view(PetOwnership.WILD, DayPhase.NIGHT, 1)).ok());
    }

    /** Both a weed and a mushroom {@code blocksEast} of home. */
    private static TestView view(PetOwnership ownership, DayPhase phase, int blocksEast) {
        Optional<GlobalPos> target = Optional.of(GlobalPos.of(OVERWORLD, HOME.pos().east(blocksEast)));
        PerceivedTargets targets = new PerceivedTargets(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), target, target, Optional.empty());
        return TestView.at(HOME, WILD_ANCHOR).ownership(ownership).phase(phase).targets(targets);
    }

    private static BlockWorkErrand.Work workAt(int blocksEast) {
        return new BlockWorkErrand.Work(GlobalPos.of(OVERWORLD, HOME.pos().east(blocksEast)));
    }

    private static PetTask slip(ResourceLocation counter) {
        return new PetTask(ResourceLocation.fromNamespaceAndPath("chiikawa", "test"), ResourceLocation.fromNamespaceAndPath("chiikawa", "farmer"),
            counter, PetTask.NO_ICON, 10, ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("chiikawa", "pet_task/test")), 0);
    }
}
