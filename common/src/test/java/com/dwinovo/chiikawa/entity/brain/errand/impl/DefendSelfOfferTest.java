package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.combat.Arms;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestTargets;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** When a pet takes a weapon out of its bag: it is attacked, has none in its hand, and may swap to one. */
class DefendSelfOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final UUID FOE = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
    private static final Arms.Weapon SWORD = new Arms.Weapon(2, false);
    private static final Arms.Weapon BOW = new Arms.Weapon(3, true);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    /** A pet by its home, a foe three blocks off that it fights for these reasons, and a spare weapon in its bag. */
    private static TestView attacked(Arms.Weapon weapon, Ground... grounds) {
        return TestView.at(HOME, TestAnchors.free(HOME)).arms(new Arms(false, Optional.ofNullable(weapon)))
            .targets(new PerceivedTargets(Optional.of(TestTargets.foe(FOE, east(3), grounds)), Map.of(), Optional.empty()));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    @Test
    void aPetThatIsHitTakesUpTheWeaponInItsBagAndFightsThatFoe() {
        TestView view = attacked(SWORD, Ground.SELF);

        assertEquals(new DefendSelfErrand.Defence(FOE, SWORD), TestErrands.planOf(ErrandIds.DEFEND_SELF, view));
        assertEquals(Optional.of(east(3)), TestErrands.planOf(ErrandIds.DEFEND_SELF, view).destination(view));
    }

    @Test
    void itTakesUpABowToShootWithAndASwordToStrikeWith() {
        assertEquals(new DefendSelfErrand.Defence(FOE, BOW), TestErrands.planOf(ErrandIds.DEFEND_SELF, attacked(BOW, Ground.SELF)));
    }

    @Test
    void whatIsAboutItsOwnerIsAsMuchAReasonAsWhatIsAboutItself() {
        assertEquals(1, TestErrands.offers(ErrandIds.DEFEND_SELF, attacked(SWORD, Ground.OWNER)).size());
    }

    @Test
    void whatIsOnlyNearbyOrOnlyAHuntIsNotAnAttack() {
        for (Ground ground : new Ground[] {Ground.NEARBY, Ground.HUNT}) {
            assertEquals("intent.chiikawa.fail.no_attacker",
                TestErrands.whyNoOffer(ErrandIds.DEFEND_SELF, attacked(SWORD, ground)).reasonKey(), ground::toString);
        }
        TestView nobody = TestView.at(HOME, TestAnchors.free(HOME)).arms(new Arms(false, Optional.of(SWORD)));
        assertEquals("intent.chiikawa.fail.no_attacker", TestErrands.whyNoOffer(ErrandIds.DEFEND_SELF, nobody).reasonKey());
    }

    @Test
    void aPetAlreadyHoldingAWeaponNeedsNoSwapForItsJobFightsAlready() {
        TestView view = attacked(SWORD, Ground.SELF).arms(new Arms(true, Optional.of(SWORD)));

        assertEquals("intent.chiikawa.fail.armed", TestErrands.whyNoOffer(ErrandIds.DEFEND_SELF, view).reasonKey());
    }

    @Test
    void aPetWithNothingToSwapToOrToldNotToHasNoWeaponToTakeUp() {
        assertEquals("intent.chiikawa.fail.no_spare_weapon",
            TestErrands.whyNoOffer(ErrandIds.DEFEND_SELF, attacked(null, Ground.SELF)).reasonKey());
    }

    @Test
    void theSwapChangesThePetsJobAndTheErrandGoesOnAllTheSame() {
        TestView swapped = attacked(SWORD, Ground.SELF).arms(new Arms(true, Optional.empty()));

        assertTrue(TestErrands.stillWanted(ErrandIds.DEFEND_SELF, new DefendSelfErrand.Defence(FOE, SWORD), swapped).ok(),
            "the errand ended with the swap that it began with");
    }
}
