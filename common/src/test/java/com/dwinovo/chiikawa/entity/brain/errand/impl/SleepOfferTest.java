package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.step.Sleep;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
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

/**
 * When a pet lies down to sleep and where: at night, near home, for a pet that is free or wild, and for a pet at heel while its
 * owner sleeps; on a bed that is free if there is one within its reach, and on the ground otherwise.
 */
class SleepOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")), ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final GlobalPos NEAR_BED = GlobalPos.of(OVERWORLD, new BlockPos(6, 64, 0));
    private static final GlobalPos FAR_BED = GlobalPos.of(OVERWORLD, new BlockPos(40, 64, 0));
    private static final GlobalPos OWNERS_BEDSIDE = GlobalPos.of(OVERWORLD, new BlockPos(3, 64, 3));

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    private static TestView free() {
        return TestView.at(HOME, TestAnchors.free(HOME)).phase(DayPhase.NIGHT);
    }

    private static TestView atHeel() {
        return TestView.at(HOME, TestAnchors.following(HOME)).ownerPos(HOME);
    }

    @Test
    void aFreePetSleepsAtNightOnTheGroundAtHomeWhenThereIsNoBed() {
        assertEquals(new SleepErrand.OnGround(HOME, Sleep.Wake.AT_MORNING), TestErrands.planOf(ErrandIds.SLEEP, free()));
    }

    @Test
    void aFreePetSleepsOnABedThatIsFreeAndWithinItsReach() {
        assertEquals(new SleepErrand.OnBed(NEAR_BED, Sleep.Wake.AT_MORNING), TestErrands.planOf(ErrandIds.SLEEP, free().freeBed(NEAR_BED)));
    }

    @Test
    void aBedBeyondTheAreaAPetActsInIsNoBedToSleepOnAndThePetSleepsAtHome() {
        assertEquals(new SleepErrand.OnGround(HOME, Sleep.Wake.AT_MORNING), TestErrands.planOf(ErrandIds.SLEEP, free().freeBed(FAR_BED)));
    }

    @Test
    void aWildPetSleepsAtNightToo() {
        TestView wild = TestView.at(HOME, TestAnchors.wild(HOME)).phase(DayPhase.NIGHT).ownership(PetOwnership.WILD);

        assertEquals(new SleepErrand.OnGround(HOME, Sleep.Wake.AT_MORNING), TestErrands.planOf(ErrandIds.SLEEP, wild));
    }

    @Test
    void aPetDoesNotSleepAnyOtherTimeOfDay() {
        for (DayPhase phase : new DayPhase[] {DayPhase.MORNING, DayPhase.DAY, DayPhase.EVENING}) {
            assertEquals("intent.chiikawa.fail.not_night", TestErrands.whyNoOffer(ErrandIds.SLEEP, free().phase(phase)).reasonKey(), phase::name);
        }
    }

    @Test
    void aPetWhereBedsAreNoUseHasNoNightToSleepThrough() {
        assertEquals("intent.chiikawa.fail.not_night", TestErrands.whyNoOffer(ErrandIds.SLEEP, free().whereBedsAreNoUse()).reasonKey());
    }

    @Test
    void aPetAtHeelSleepsOnlyWhileItsOwnerDoesWhateverTheHourAndWhereItStandsNotInTheirBed() {
        for (DayPhase phase : DayPhase.values()) {
            assertEquals("intent.chiikawa.fail.owner_awake", TestErrands.whyNoOffer(ErrandIds.SLEEP, atHeel().phase(phase)).reasonKey(), phase::name);
            assertEquals(new SleepErrand.OnGround(HOME, Sleep.Wake.WITH_OWNER),
                TestErrands.planOf(ErrandIds.SLEEP, atHeel().phase(phase).ownerAsleepBeside(OWNERS_BEDSIDE)), phase::name);
        }
    }

    @Test
    void aPetAtHeelLiesBesideItsOwnersBedAndOnAFreeBedIfThereIsOne() {
        assertEquals(new SleepErrand.OnBed(NEAR_BED, Sleep.Wake.WITH_OWNER),
            TestErrands.planOf(ErrandIds.SLEEP, atHeel().ownerAsleepBeside(OWNERS_BEDSIDE).freeBed(NEAR_BED)));
    }

    @Test
    void aFreePetWhoseOwnerSleepsStillSleepsAtItsHomeAndWakesWithTheMorning() {
        assertEquals(new SleepErrand.OnGround(HOME, Sleep.Wake.AT_MORNING),
            TestErrands.planOf(ErrandIds.SLEEP, free().ownerAsleepBeside(OWNERS_BEDSIDE)));
    }

    @Test
    void thePlanGoesToWhereThePetLiesDownSoThatTheAreaItActsInIsAskedAboutIt() {
        TestView view = free().freeBed(NEAR_BED);

        assertEquals(Optional.of(NEAR_BED), new SleepErrand.OnBed(NEAR_BED, Sleep.Wake.AT_MORNING).destination(view));
        assertEquals(Optional.of(HOME), new SleepErrand.OnGround(HOME, Sleep.Wake.AT_MORNING).destination(view));
    }

    @Test
    void aPetKeepsToItsSleepUntilTheStepWakesIt() {
        // Waking is the step's, which knows the hour and the owner; the errand has no say in it.
        assertTrue(TestErrands.stillWanted(ErrandIds.SLEEP, new SleepErrand.OnGround(HOME, Sleep.Wake.AT_MORNING), free()).ok());
        assertTrue(TestErrands.stillWanted(ErrandIds.SLEEP, new SleepErrand.OnGround(HOME, Sleep.Wake.AT_MORNING),
            free().phase(DayPhase.DAY)).ok());
    }
}
