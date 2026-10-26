package com.dwinovo.chiikawa.entity.brain.errand.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.Duel;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
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

        for (ResourceLocation errand : new ResourceLocation[] {WorkData.MELEE, WorkData.RANGED}) {
            assertEquals(new Duel(FOE), TestErrands.planOf(errand, view), errand::toString);
            assertEquals(Optional.of(east(6)), TestErrands.planOf(errand, view).destination(view), errand::toString);
        }
    }

    @Test
    void withoutAFoeThereIsNothingToFightAndWhyIsSaid() {
        TestView nobody = TestView.at(HOME, FREE).armedWithArrows();

        assertEquals("intent.chiikawa.fail.no_attack_target", TestErrands.whyNoOffer(WorkData.MELEE, nobody).reasonKey());
        assertEquals("intent.chiikawa.fail.no_attack_target", TestErrands.whyNoOffer(WorkData.RANGED, nobody).reasonKey());
    }

    @Test
    void theReasonThereIsNoFoeIsTheOneTheWorkNames() {
        Work<Duel, ErrandTunings.Footwork> work = new Work<>(WorkActions.STRIKE, ErrandCategory.COMBAT, Optional.empty(),
            List.of(), Optional.of(new WorkTarget.Foe("no_prey")));
        ErrandTunings.Footwork tuning = (ErrandTunings.Footwork) ErrandSettings.of(WorkData.MELEE).orElseThrow().tuning();

        assertEquals("intent.chiikawa.fail.no_prey",
            new WorkErrand<>(WorkData.MELEE, work).offers(TestView.at(HOME, FREE), tuning).check().reasonKey());
    }

    @Test
    void anArcherNeedsArrowsAndASwordsmanDoesNot() {
        TestView noArrows = fighting();

        assertEquals(1, TestErrands.offers(WorkData.MELEE, noArrows).size());
        assertEquals("intent.chiikawa.fail.no_arrows", TestErrands.whyNoOffer(WorkData.RANGED, noArrows).reasonKey());
    }

    @Test
    void aFightDoesNotStartWhileThePetHoldsOffItsNextAttack() {
        TestView resting = fighting().armedWithArrows().attackResting();

        assertEquals("intent.chiikawa.fail.attack_cooling_down", TestErrands.whyNoOffer(WorkData.MELEE, resting).reasonKey());
        assertEquals("intent.chiikawa.fail.attack_cooling_down", TestErrands.whyNoOffer(WorkData.RANGED, resting).reasonKey());
    }

    @Test
    void aFightGoesOnBetweenSwingsAndShotsForAsLongAsAnArcherHasArrows() {
        Duel duel = new Duel(FOE);
        TestView withArrows = fighting().attackResting().armedWithArrows();
        TestView without = fighting().attackResting();

        assertTrue(TestErrands.stillWanted(WorkData.MELEE, duel, without).ok());
        assertTrue(TestErrands.stillWanted(WorkData.RANGED, duel, withArrows).ok());
        assertEquals("intent.chiikawa.fail.no_arrows", TestErrands.stillWanted(WorkData.RANGED, duel, without).reasonKey());
    }

    /** A pet with a foe six blocks off. */
    private static TestView fighting() {
        return TestView.at(HOME, FREE).targets(new PerceivedTargets(Optional.of(new AttackTarget(FOE, east(6))),
            Map.of(), Optional.empty()));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }
}
