package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.CombatStance;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Which grounds the owner's orders let a pet fight on, the rule that says what the attack sensor may put on its list.
 * Pure, so the whole table can be read and tested at once.
 *
 * <p>A pet told to sit sits through everything, and one told not to fight does not, whatever it could. Standing up
 * for itself and its owner is the vanilla wolf's: it answers what hurt it and what is about its owner. Beyond that the
 * stance {@link CombatStance#CLEAR} has it deal with the hostile things it sees nearby: at its owner's side, or in the wild, as
 * a guard, and let loose in its area as a hunt, which matters less, so that a fencer clearing the yard is never what keeps it
 * from the work it is for. Hunting is also what a hunting slip is for, whatever the stance says about a pet that is let loose.
 */
public final class FoeCandidates {
    private FoeCandidates() {
    }

    /**
     * @param stance how far the owner has told the pet to take part in a fight
     * @param directive where the owner has told it to be
     * @param ownership who it belongs to; a wild pet has no directive to go by
     * @param carriesSlaySlip whether the pet carries a slip for putting down monsters
     * @param fights whether the pet's job has work done to a foe; one that has not fights only for itself and its owner, in
     *               the way it has, and has no use for the foes of a hunt or of a patch to guard
     * @return the grounds it may fight on now, none for a pet that is not to fight
     */
    public static Set<Ground> of(CombatStance stance, PetDirective directive, PetOwnership ownership, boolean carriesSlaySlip,
            boolean fights) {
        boolean wild = ownership instanceof PetOwnership.Wild;
        if (!wild && directive == PetDirective.STAY) {
            return Set.of();
        }
        boolean free = !wild && directive == PetDirective.FREE;
        EnumSet<Ground> grounds = EnumSet.noneOf(Ground.class);
        if (stance != CombatStance.NONE) {
            grounds.add(Ground.SELF);
            grounds.add(Ground.OWNER);
        }
        if (stance == CombatStance.CLEAR) {
            grounds.add(free ? Ground.HUNT : Ground.NEARBY);
        }
        if (free && carriesSlaySlip) {
            grounds.add(Ground.HUNT);
        }
        if (!fights) {
            grounds.retainAll(EnumSet.of(Ground.SELF, Ground.OWNER));
        }
        return Collections.unmodifiableSet(grounds);
    }
}
