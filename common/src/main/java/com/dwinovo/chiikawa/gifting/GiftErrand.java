package com.dwinovo.chiikawa.gifting;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.init.InitMemory;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;

/**
 * When a pet may set off with a present, and go on with it, said once for everything that
 * asks: the intent that chooses the errand and the behavior that makes it, which must not
 * disagree. A behavior that starts again in the tick after it hands a present over, before the
 * intent has been chosen anew, has to find the answer is no.
 *
 * <p>A pet only walks a present to an owner who is within the area it acts in, the same one its
 * other errands keep to: for one that roams free, round its home; for one that follows, the owner
 * is the middle of it, so they always are. It sets off for one within reach and goes on for one
 * within the leash, so a present that was fetched for an owner who walked a little further is not
 * dropped for it, but one whose owner has left is put back.
 */
public final class GiftErrand {
    private GiftErrand() {
    }

    /**
     * @param anchor where the pet acts
     * @param owner where its owner is
     * @param starting whether the pet is setting off, which asks for the reach, or has set off,
     *                 which allows the leash
     * @return whether the owner is in the area the pet acts in
     */
    public static boolean ownerAtHome(PetAnchor anchor, GlobalPos owner, boolean starting) {
        return starting ? anchor.withinReach(owner) : anchor.withinLeash(owner);
    }

    /** Whether the pet is carrying a present to an owner it may still walk it to. */
    public static boolean mayContinue(AbstractPet pet) {
        return pet.getOwner() instanceof Player owner && !pet.getPendingGift().isEmpty()
            && ownerAtHome(pet, owner, false);
    }

    /**
     * Whether the pet has another present in mind: its owner is at home to be given it, its last
     * present was long enough ago, and there is something in its bag it may give.
     */
    public static boolean thinksOfAnother(AbstractPet pet) {
        return pet.getOwner() instanceof Player owner
            && ownerAtHome(pet, owner, true)
            && !pet.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get())
            && !GiftPool.of(pet).isEmpty();
    }

    /** Whether the pet may set off now: it is carrying a present it may go on with, or it thinks of another. */
    public static boolean mayBegin(AbstractPet pet) {
        return mayContinue(pet) || thinksOfAnother(pet);
    }

    private static boolean ownerAtHome(AbstractPet pet, Player owner, boolean starting) {
        PetAnchor anchor = PetConstraints.anchorOf(pet, PetOwnership.of(pet));
        return ownerAtHome(anchor, GlobalPos.of(owner.level().dimension(), owner.blockPosition()), starting);
    }
}
