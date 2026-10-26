package com.dwinovo.chiikawa.whistle;

import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import java.util.UUID;

/**
 * Who a whistle is for. Pure, so the rules of it can be read and checked without a world:
 * {@link PetWhistle} asks it about every living pet near the blower and acts on the answers.
 */
public final class WhistleHearing {
    /** What a pet makes of a whistle. */
    public enum Hearing {
        /** The blower's own, or its owner's friend's: it takes the order. */
        OWNED,
        /** Nobody's: it comes over to see who whistled, and stays nobody's. */
        WILD,
        /** Out of earshot, in another world, someone else's, or busy. */
        DEAF
    }

    private WhistleHearing() {
    }

    /**
     * @param whistler who blew
     * @param order what they blew for
     * @param ownership whose the pet is
     * @param allied whether the pet counts the blower as on its side, as it does a friend of its owner, see {@code PetFriends}
     * @param sameLevel whether the pet is in the blower's world
     * @param distanceSqr the squared distance between them
     * @param range how far a whistle carries
     * @param seatedAtExam whether the pet is at its desk, writing, which a whistle does not
     *                     get it up from
     */
    public static Hearing of(UUID whistler, PetDirective order, PetOwnership ownership, boolean allied, boolean sameLevel,
                             double distanceSqr, double range, boolean seatedAtExam) {
        if (!sameLevel || distanceSqr > range * range || seatedAtExam) {
            return Hearing.DEAF;
        }
        if (ownership instanceof PetOwnership.Owned owned) {
            return owned.ownerId().equals(whistler) || allied ? Hearing.OWNED : Hearing.DEAF;
        }
        // Curiosity is for a call to come over: being told to sit or to roam means nothing to a stranger.
        return order == PetDirective.FOLLOW ? Hearing.WILD : Hearing.DEAF;
    }
}
