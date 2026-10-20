package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Fighting with a sword and with a bow: who the pet sets off against and what keeps it at it. */
class CombatOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FREE = TestAnchors.free(HOME);
    private static final UUID FOE = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void theFoeTheSensorNamesIsTheOneThePetSetsOffAgainstAndWhereItGoes() {
        TestView view = fighting().armedWithArrows();

        for (ResourceLocation errand : new ResourceLocation[] {ErrandIds.MELEE, ErrandIds.RANGED}) {
            assertEquals(new CombatErrand.Duel(FOE), TestErrands.planOf(errand, view), errand::toString);
            assertEquals(Optional.of(east(6)), TestErrands.planOf(errand, view).destination(view), errand::toString);
        }
    }

    @Test
    void withoutAFoeThereIsNothingToFightAndWhyIsSaid() {
        TestView nobody = TestView.at(HOME, FREE).armedWithArrows();

        assertEquals("intent.chiikawa.fail.no_attack_target", TestErrands.whyNoOffer(ErrandIds.MELEE, nobody).reasonKey());
        assertEquals("intent.chiikawa.fail.no_attack_target", TestErrands.whyNoOffer(ErrandIds.RANGED, nobody).reasonKey());
    }

    @Test
    void anArcherNeedsArrowsAndASwordsmanDoesNot() {
        TestView noArrows = fighting();

        assertEquals(1, TestErrands.offers(ErrandIds.MELEE, noArrows).size());
        assertEquals("intent.chiikawa.fail.no_arrows", TestErrands.whyNoOffer(ErrandIds.RANGED, noArrows).reasonKey());
    }

    @Test
    void aFightDoesNotStartWhileThePetHoldsOffItsNextAttack() {
        TestView resting = fighting().armedWithArrows().attackResting();

        assertEquals("intent.chiikawa.fail.attack_cooling_down", TestErrands.whyNoOffer(ErrandIds.MELEE, resting).reasonKey());
        assertEquals("intent.chiikawa.fail.attack_cooling_down", TestErrands.whyNoOffer(ErrandIds.RANGED, resting).reasonKey());
    }

    @Test
    void aFightGoesOnBetweenSwingsAndShotsForAsLongAsAnArcherHasArrows() {
        CombatErrand.Duel duel = new CombatErrand.Duel(FOE);
        TestView withArrows = fighting().attackResting().armedWithArrows();
        TestView without = fighting().attackResting();

        assertTrue(TestErrands.stillWanted(ErrandIds.MELEE, duel, without).ok());
        assertTrue(TestErrands.stillWanted(ErrandIds.RANGED, duel, withArrows).ok());
        assertEquals("intent.chiikawa.fail.no_arrows", TestErrands.stillWanted(ErrandIds.RANGED, duel, without).reasonKey());
    }

    /** A pet with a foe six blocks off. */
    private static TestView fighting() {
        return TestView.at(HOME, FREE).targets(new PerceivedTargets(Optional.of(new AttackTarget(FOE, east(6))),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }
}
