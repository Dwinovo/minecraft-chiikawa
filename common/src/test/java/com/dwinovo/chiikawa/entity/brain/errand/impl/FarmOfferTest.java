package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.rest.SubjectRests;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.Site;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestTargets;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The farmer's errands that work a block the sensor remembers: reaping, planting, and putting the crop away. */
class FarmOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FREE = TestAnchors.free(HOME);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void theBlockTheSensorRemembersIsWhereThePetGoesAndWhatItRestsOn() {
        TestView view = remembered(east(3), east(5), east(7));

        for (ResourceLocation errand : new ResourceLocation[] {WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER}) {
            assertEquals(1, TestErrands.offers(errand, view).size(), errand::toString);
        }
        assertEquals(Optional.of(east(3)), TestErrands.planOf(WorkData.HARVEST, view).destination(view));
        assertEquals(Optional.of(east(5)), TestErrands.planOf(WorkData.PLANT, view).destination(view));
        assertEquals(Optional.of(east(7)), TestErrands.planOf(WorkData.DELIVER, view).destination(view));
        assertEquals(Optional.of(SubjectRests.subject(east(3).pos())), TestErrands.planOf(WorkData.HARVEST, view).subject());
        assertEquals(Optional.of(SubjectRests.subject(east(7).pos())), TestErrands.planOf(WorkData.DELIVER, view).subject());
    }

    @Test
    void aBlockIsRestedBySubjectSoAnotherCropIsNotHeldBackByIt() {
        assertEquals("3,64,0", SubjectRests.subject(east(3).pos()));
        assertNotEquals(SubjectRests.subject(east(3).pos()), SubjectRests.subject(east(4).pos()));
    }

    @Test
    void withoutABlockThereIsNothingToOfferAndWhyIsSaid() {
        TestView nothing = TestView.at(HOME, FREE);

        assertEquals("intent.chiikawa.fail.no_crop", TestErrands.whyNoOffer(WorkData.HARVEST, nothing).reasonKey());
        assertEquals("intent.chiikawa.fail.no_farmland", TestErrands.whyNoOffer(WorkData.PLANT, nothing).reasonKey());
        assertEquals("intent.chiikawa.fail.no_container", TestErrands.whyNoOffer(WorkData.DELIVER, nothing).reasonKey());
    }

    @Test
    void aPlanterWithNoSeedLacksSeedsAndOneWithSeedsButNoSoilLacksNothing() {
        Check noSeeds = TestErrands.whyNoOffer(WorkData.PLANT, TestView.at(HOME, FREE));
        Check noSoil = TestErrands.whyNoOffer(WorkData.PLANT, TestView.at(HOME, FREE).carryingSeeds());

        // The reason is the one the work names either way, and with it what is short when it is the seeds.
        assertEquals("intent.chiikawa.fail.no_farmland", noSeeds.reasonKey());
        assertEquals(Optional.of(LackKey.tag(InitTag.ENTITY_PLANT_CROPS)), noSeeds.lack());
        assertEquals("intent.chiikawa.fail.no_farmland", noSoil.reasonKey());
        assertEquals(Optional.empty(), noSoil.lack());
        assertEquals(Optional.empty(), TestErrands.whyNoOffer(WorkData.HARVEST, TestView.at(HOME, FREE)).lack());
    }

    @Test
    void aWalkToACropGoesOnOnlyWhileTheSensorStillRemembersThatCrop() {
        Site toThree = new Site(east(3));

        assertTrue(TestErrands.stillWanted(WorkData.HARVEST, toThree, remembered(east(3), east(5), east(7))).ok());
        // The crop is gone, or the sensor has moved on to another: either way the pet's crop is not the one remembered.
        assertEquals("intent.chiikawa.fail.no_crop",
            TestErrands.stillWanted(WorkData.HARVEST, toThree, TestView.at(HOME, FREE)).reasonKey());
        assertEquals("intent.chiikawa.fail.no_crop", TestErrands.stillWanted(WorkData.HARVEST, toThree,
            remembered(east(4), east(5), east(7))).reasonKey());
    }

    @Test
    void aDeliveryIsNotAskedWhetherItWantsToGoOnWhenItsBagHasJustBeenEmptied() {
        Site trip = new Site(east(7));

        // The sensor forgets the container once there is nothing to put in it, which is the delivery done and not given up.
        assertTrue(TestErrands.stillWanted(WorkData.DELIVER, trip, TestView.at(HOME, FREE)).ok());
        assertEquals(ErrandCategory.WORK, TestErrands.errand(WorkData.DELIVER).category());
    }

    /** A crop, soil and a container, each as far east of home as given. */
    private static TestView remembered(GlobalPos crop, GlobalPos soil, GlobalPos container) {
        return TestView.at(HOME, FREE).targets(TestTargets.of(Optional.empty(), Optional.of(crop), Optional.of(soil),
            Optional.of(container), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }
}
