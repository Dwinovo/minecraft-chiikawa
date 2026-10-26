package com.dwinovo.chiikawa.entity.brain.combat;

import static com.dwinovo.chiikawa.entity.brain.combat.Arms.Kind.BOW;
import static com.dwinovo.chiikawa.entity.brain.combat.Arms.Kind.OTHER;
import static com.dwinovo.chiikawa.entity.brain.combat.Arms.Kind.SWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.combat.Arms.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** What a pet has to fight with, from what each pocket of its backpack holds. */
class ArmsTest {
    /** A hand, a pocket for the bag it wears, and a few pockets for things. */
    private static List<Kind> backpack(Kind hand, Kind... pockets) {
        List<Kind> all = new ArrayList<>(List.of(hand, OTHER));
        all.addAll(List.of(pockets));
        return all;
    }

    @Test
    void aSwordInHandIsAWeaponAndABowInHandIsOneOnlyWithAnArrowToLoose() {
        assertTrue(Arms.choose(SWORD, backpack(SWORD), false, true).wielding());
        assertTrue(Arms.choose(BOW, backpack(BOW), true, true).wielding());
        assertFalse(Arms.choose(BOW, backpack(BOW), false, true).wielding());
        assertFalse(Arms.choose(OTHER, backpack(OTHER), true, true).wielding());
    }

    @Test
    void aWeaponInTheBagIsOneToTakeUpAndTheFirstPocketOfThemIsTheOne() {
        Arms arms = Arms.choose(OTHER, backpack(OTHER, OTHER, BOW, SWORD), true, true);

        assertFalse(arms.wielding());
        assertEquals(Optional.of(new Arms.Weapon(3, true)), arms.swap());
        assertEquals(Optional.of(new Arms.Weapon(4, false)), Arms.choose(OTHER, backpack(OTHER, OTHER, OTHER, SWORD), true, true).swap());
    }

    @Test
    void aBowInTheBagWithNothingToLooseIsNoWeapon() {
        assertEquals(Optional.empty(), Arms.choose(OTHER, backpack(OTHER, BOW), false, true).swap());
        assertEquals(Optional.of(new Arms.Weapon(3, false)), Arms.choose(OTHER, backpack(OTHER, BOW, SWORD), false, true).swap());
    }

    @Test
    void anOwnerWhoSaysNoSwapMeansNoWeaponToTakeUpWhatIsInTheBag() {
        Arms arms = Arms.choose(OTHER, backpack(OTHER, SWORD), true, false);

        assertEquals(Optional.empty(), arms.swap());
    }

    @Test
    void thePocketForTheBagThePetWearsIsNotOneToSwapFrom() {
        assertEquals(Optional.empty(), Arms.choose(OTHER, List.of(OTHER, SWORD), true, true).swap());
    }

    @Test
    void theHandIsNotAPocketToSwapFrom() {
        assertEquals(Optional.empty(), Arms.choose(SWORD, backpack(SWORD), true, true).swap());
    }
}
