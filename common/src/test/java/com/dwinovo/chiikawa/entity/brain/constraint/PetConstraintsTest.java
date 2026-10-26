package com.dwinovo.chiikawa.entity.brain.constraint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.CombatStance;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRules;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.testing.TestAnchors;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class PetConstraintsTest {
    private static final ResourceKey<Registry<Level>> DIMENSION_REGISTRY =
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension"));
    private static final ResourceKey<Level> OVERWORLD =
        ResourceKey.create(DIMENSION_REGISTRY, ResourceLocation.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER =
        ResourceKey.create(DIMENSION_REGISTRY, ResourceLocation.withDefaultNamespace("the_nether"));

    private static final PetOwnership OWNED = new PetOwnership.Owned(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final GlobalPos PET = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final GlobalPos OWNER = GlobalPos.of(OVERWORLD, new BlockPos(5, 64, 0));
    private static final double TELEPORT = TestAnchors.SETTINGS.follow().teleport();
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(-8, 64, 3));

    // ---- anchor table ----------------------------------------------------------

    @Test
    void followingAnOnlineOwnerAnchorsOnTheOwner() {
        PetAnchor anchor = anchor(PetDirective.FOLLOW, OWNED, Optional.of(OWNER), Optional.empty(), false);

        assertEquals(TestAnchors.following(OWNER), anchor);
    }

    @Test
    void followingWithoutOwnerStaysOnThePetWithNoReachOrLeash() {
        PetAnchor offline = anchor(PetDirective.FOLLOW, OWNED, Optional.empty(), Optional.empty(), false);
        PetAnchor otherDimension = anchor(PetDirective.FOLLOW, OWNED,
            Optional.of(GlobalPos.of(NETHER, OWNER.pos())), Optional.empty(), false);

        PetAnchor expected = new PetAnchor(PET, 0.0, PetAnchor.UNLEASHED, false, true);
        assertEquals(expected, offline);
        assertEquals(expected, otherDimension);
    }

    @Test
    void leashedOrRidingPetDoesNotFollowItsOwner() {
        PetAnchor anchor = anchor(PetDirective.FOLLOW, OWNED, Optional.of(OWNER), Optional.empty(), true);

        assertEquals(new PetAnchor(PET, 0.0, PetAnchor.UNLEASHED, false, true), anchor);
    }

    @Test
    void stayingPetCannotMove() {
        PetAnchor anchor = anchor(PetDirective.STAY, OWNED, Optional.of(OWNER), Optional.of(HOME), false);

        assertEquals(new PetAnchor(PET, 0.0, PetAnchor.UNLEASHED, false, false), anchor);
    }

    @Test
    void freePetAnchorsOnItsHome() {
        PetAnchor anchor = anchor(PetDirective.FREE, OWNED, Optional.of(OWNER), Optional.of(HOME), false);

        assertEquals(TestAnchors.free(HOME), anchor);
    }

    @Test
    void freePetWithoutUsableHomeAnchorsOnItself() {
        PetAnchor noHome = anchor(PetDirective.FREE, OWNED, Optional.empty(), Optional.empty(), false);
        PetAnchor homeElsewhere = anchor(PetDirective.FREE, OWNED, Optional.empty(),
            Optional.of(GlobalPos.of(NETHER, HOME.pos())), false);

        PetAnchor expected = TestAnchors.free(PET);
        assertEquals(expected, noHome);
        assertEquals(expected, homeElsewhere);
    }

    @Test
    void leashedFreePetKeepsItsReachButIsNotPulledHome() {
        PetAnchor anchor = anchor(PetDirective.FREE, OWNED, Optional.empty(), Optional.of(HOME), true);

        assertEquals(new PetAnchor(HOME, TestAnchors.SETTINGS.free().reach(), PetAnchor.UNLEASHED, false, true), anchor);
    }

    @Test
    void wildPetRoamsAroundItsSpawnHomeWhateverItsDirective() {
        PetAnchor expected = TestAnchors.wild(HOME);
        for (PetDirective directive : PetDirective.values()) {
            assertEquals(expected, anchor(directive, PetOwnership.WILD, Optional.empty(), Optional.of(HOME), false));
        }
    }

    @Test
    void wildPetWithoutUsableHomeRoamsAroundItself() {
        PetAnchor noHome = anchor(PetDirective.FOLLOW, PetOwnership.WILD, Optional.empty(), Optional.empty(), false);
        PetAnchor homeElsewhere = anchor(PetDirective.FREE, PetOwnership.WILD, Optional.empty(),
            Optional.of(GlobalPos.of(NETHER, HOME.pos())), false);

        PetAnchor expected = TestAnchors.wild(PET);
        assertEquals(expected, noHome);
        assertEquals(expected, homeElsewhere);
    }

    @Test
    void leashedWildPetKeepsItsReachButIsNotPulledHome() {
        PetAnchor anchor = anchor(PetDirective.FREE, PetOwnership.WILD, Optional.empty(), Optional.of(HOME), true);

        assertEquals(new PetAnchor(HOME, TestAnchors.SETTINGS.wild().reach(), PetAnchor.UNLEASHED, false, true), anchor);
    }

    // ---- permission table ------------------------------------------------------

    @Test
    void eachSwitchForbidsItsCategoryAndNothingElse() {
        for (PetDirective directive : PetDirective.values()) {
            for (PetRules.Toggle toggle : PetRules.Toggle.values()) {
                PetRules rules = PetRules.DEFAULT.with(toggle, false);
                for (ErrandCategory category : ErrandCategory.values()) {
                    boolean expected = category != toggle.category()
                        && PetConstraints.allows(directive, OWNED, PetRules.DEFAULT, category);
                    assertEquals(expected, PetConstraints.allows(directive, OWNED, rules, category),
                        () -> directive + " / " + toggle + " off / " + category);
                }
            }
        }
    }

    @Test
    void aSwitchOffForbidsItsCategoryEvenInTheDirectiveThatPermitsItMost() {
        assertFalse(PetConstraints.allows(PetDirective.FREE, OWNED, PetRules.DEFAULT.with(PetRules.Toggle.SHOP, false), ErrandCategory.SHOP));
        assertFalse(PetConstraints.allows(PetDirective.FOLLOW, OWNED, PetRules.DEFAULT.with(PetRules.Toggle.GIFT, false), ErrandCategory.GIFT));
        assertFalse(PetConstraints.allows(PetDirective.FOLLOW, OWNED, PetRules.DEFAULT.with(PetRules.Toggle.SOCIAL, false), ErrandCategory.SOCIAL));
        assertFalse(PetConstraints.allows(PetDirective.FOLLOW, OWNED, PetRules.DEFAULT.with(PetRules.Toggle.PHOTO, false), ErrandCategory.PHOTO));
        assertFalse(PetConstraints.allows(PetDirective.FREE, OWNED, PetRules.DEFAULT.with(PetRules.Toggle.PICK_UP, false), ErrandCategory.PICK_UP));
    }

    @Test
    void theStanceAndTheSwapForbidNothing() {
        PetRules rules = PetRules.DEFAULT.withStance(CombatStance.NONE).withSwap(false);
        for (PetDirective directive : PetDirective.values()) {
            for (ErrandCategory category : ErrandCategory.values()) {
                assertEquals(PetConstraints.allows(directive, OWNED, PetRules.DEFAULT, category),
                    PetConstraints.allows(directive, OWNED, rules, category), directive + " / " + category);
            }
        }
    }

    @Test
    void gettingOutOfHarmsWayIsPermittedWhateverThePetIsToldAndWhoeverItBelongsTo() {
        PetRules everythingOff = PetRules.DEFAULT.withStance(CombatStance.NONE).withSwap(false);
        for (PetRules.Toggle toggle : PetRules.Toggle.values()) {
            everythingOff = everythingOff.with(toggle, false);
        }
        for (PetDirective directive : PetDirective.values()) {
            for (PetOwnership ownership : List.of(OWNED, PetOwnership.WILD)) {
                assertTrue(PetConstraints.allows(directive, ownership, everythingOff, ErrandCategory.SAFETY),
                    directive + " / " + ownership);
            }
        }
    }

    @Test
    void everythingSwitchedOffStillLeavesFollowingStayingWanderingAndFighting() {
        PetRules rules = PetRules.DEFAULT;
        for (PetRules.Toggle toggle : PetRules.Toggle.values()) {
            rules = rules.with(toggle, false);
        }

        assertTrue(PetConstraints.allows(PetDirective.FOLLOW, OWNED, rules, ErrandCategory.FOLLOW_OWNER));
        assertTrue(PetConstraints.allows(PetDirective.FOLLOW, OWNED, rules, ErrandCategory.WANDER));
        assertTrue(PetConstraints.allows(PetDirective.FOLLOW, OWNED, rules, ErrandCategory.COMBAT));
        assertTrue(PetConstraints.allows(PetDirective.STAY, OWNED, rules, ErrandCategory.STAY));
        assertTrue(PetConstraints.allows(PetDirective.FREE, OWNED, rules, ErrandCategory.WORK));
    }

    @Test
    void followPermitsFollowingWanderingTakingSlipsGivingMeetingPetsExamsPhotosAndDefendingTheOwner() {
        assertPermits(PetDirective.FOLLOW, OWNED,
            EnumSet.of(ErrandCategory.FOLLOW_OWNER, ErrandCategory.WANDER, ErrandCategory.TAKE_TASK,
                ErrandCategory.GIFT, ErrandCategory.COMBAT, ErrandCategory.SOCIAL, ErrandCategory.EXAM, ErrandCategory.PHOTO,
                ErrandCategory.SAFETY));
    }

    @Test
    void stayPermitsOnlyStayingAndGettingOutOfHarmsWay() {
        assertPermits(PetDirective.STAY, OWNED, EnumSet.of(ErrandCategory.STAY, ErrandCategory.SAFETY));
    }

    @Test
    void freePermitsEverythingAPetDoesOnItsOwnTime() {
        assertPermits(PetDirective.FREE, OWNED, EnumSet.of(ErrandCategory.WANDER, ErrandCategory.WORK, ErrandCategory.FORAGE,
            ErrandCategory.COMBAT, ErrandCategory.PICK_UP, ErrandCategory.TAKE_TASK, ErrandCategory.SHOP,
            ErrandCategory.GIFT, ErrandCategory.SOCIAL, ErrandCategory.EXAM, ErrandCategory.PHOTO, ErrandCategory.SAFETY));
    }

    @Test
    void wildPermitsWanderingForagingFightsTakingSlipsAndMeetingPetsWhateverItsDirective() {
        for (PetDirective directive : PetDirective.values()) {
            assertPermits(directive, PetOwnership.WILD,
                EnumSet.of(ErrandCategory.WANDER, ErrandCategory.FORAGE, ErrandCategory.COMBAT, ErrandCategory.TAKE_TASK,
                    ErrandCategory.SOCIAL, ErrandCategory.CURIOUS, ErrandCategory.SAFETY));
        }
    }

    // ---- anchor distances ------------------------------------------------------

    @Test
    void anchorMeasuresReachLeashAndTeleportWithinItsDimension() {
        PetAnchor anchor = new PetAnchor(PET, 12.0, 16.0, true, true);

        assertTrue(anchor.withinReach(GlobalPos.of(OVERWORLD, new BlockPos(11, 64, 0))));
        assertFalse(anchor.withinReach(GlobalPos.of(OVERWORLD, new BlockPos(12, 64, 0))));
        assertTrue(anchor.withinLeash(GlobalPos.of(OVERWORLD, new BlockPos(15, 64, 0))));
        assertFalse(anchor.withinLeash(GlobalPos.of(OVERWORLD, new BlockPos(16, 64, 0))));
        assertFalse(anchor.beyondTeleport(GlobalPos.of(OVERWORLD, new BlockPos(19, 64, 0)), TELEPORT));
        assertTrue(anchor.beyondTeleport(GlobalPos.of(OVERWORLD, new BlockPos(20, 64, 0)), TELEPORT));
        assertFalse(anchor.withinReach(GlobalPos.of(NETHER, PET.pos())));
    }

    @Test
    void onlyAnAnchorThatFollowsTheOwnerTeleports() {
        PetAnchor home = TestAnchors.free(PET);

        assertFalse(home.beyondTeleport(GlobalPos.of(OVERWORLD, new BlockPos(100, 64, 0)), TELEPORT));
    }

    @Test
    void withoutSettingsNoPetHasAnAreaAndEveryOneStaysWhereItIs() {
        PetAnchor stays = new PetAnchor(PET, 0.0, PetAnchor.UNLEASHED, false, false);

        for (PetDirective directive : PetDirective.values()) {
            assertEquals(stays, PetConstraints.anchorOf(directive, OWNED, PET, Optional.of(OWNER), Optional.of(HOME), false,
                Optional.empty()));
            assertEquals(stays, PetConstraints.anchorOf(directive, PetOwnership.WILD, PET, Optional.empty(),
                Optional.of(HOME), false, Optional.empty()));
        }
    }

    @Test
    void unleashedAnchorNeverPullsAndZeroReachNeverStarts() {
        PetAnchor anchor = new PetAnchor(PET, 0.0, PetAnchor.UNLEASHED, false, true);

        assertFalse(anchor.withinReach(PET));
        assertTrue(anchor.withinLeash(GlobalPos.of(OVERWORLD, new BlockPos(100000, 64, 0))));
        assertFalse(anchor.beyondTeleport(GlobalPos.of(OVERWORLD, new BlockPos(100000, 64, 0)), TELEPORT));
    }

    private static PetAnchor anchor(PetDirective directive, PetOwnership ownership, Optional<GlobalPos> ownerPos,
            Optional<GlobalPos> home, boolean tethered) {
        return PetConstraints.anchorOf(directive, ownership, PET, ownerPos, home, tethered, Optional.of(TestAnchors.SETTINGS));
    }

    private static void assertPermits(PetDirective directive, PetOwnership ownership, Set<ErrandCategory> permitted) {
        for (ErrandCategory category : ErrandCategory.values()) {
            assertEquals(permitted.contains(category), PetConstraints.allows(directive, ownership, PetRules.DEFAULT, category),
                () -> directive + " / " + ownership + " / " + category);
        }
    }
}
