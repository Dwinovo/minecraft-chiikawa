package com.dwinovo.chiikawa.entity.brain.sensor;

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
 * <p>A pet told to sit sits through everything. Standing up for itself and its owner is the vanilla wolf's: it answers what
 * hurt it and what is about its owner. Beyond that it deals with the hostile things it sees nearby: at its owner's side, or
 * in the wild, as a guard, and let loose in its area as a hunt, which matters less, so that a fencer clearing the yard is never
 * what keeps it from the work it is for. What it fights with is its weapon's to say, and an owner who would have it keep out of
 * fights takes the weapon away.
 */
public final class FoeCandidates {
    private FoeCandidates() {
    }

    /**
     * @param directive where the owner has told the pet to be
     * @param ownership who it belongs to; a wild pet has no directive to go by
     * @param fights whether the pet's job has work done to a foe; one that has not fights only for itself and its owner, in
     *               the way it has, and has no use for the foes of a hunt or of a patch to guard
     * @return the grounds it may fight on now, none for a pet that is told to sit
     */
    public static Set<Ground> of(PetDirective directive, PetOwnership ownership, boolean fights) {
        boolean wild = ownership instanceof PetOwnership.Wild;
        if (!wild && directive == PetDirective.STAY) {
            return Set.of();
        }
        boolean free = !wild && directive == PetDirective.FREE;
        EnumSet<Ground> grounds = EnumSet.of(Ground.SELF, Ground.OWNER, free ? Ground.HUNT : Ground.NEARBY);
        if (!fights) {
            grounds.retainAll(EnumSet.of(Ground.SELF, Ground.OWNER));
        }
        return Collections.unmodifiableSet(grounds);
    }
}
