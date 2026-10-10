package com.dwinovo.chiikawa.gifting;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import net.minecraft.world.entity.player.Player;

/**
 * When a pet may set off with a present, said once for everything that asks: the intent that
 * chooses the errand and the behavior that makes it, which must not disagree. A behavior that
 * starts again in the tick after it hands a present over, before the intent has been chosen
 * anew, has to find the answer is no.
 */
public final class GiftErrand {
    private GiftErrand() {
    }

    /** Whether the pet is already carrying a present to an owner who can be given it. */
    public static boolean carrying(AbstractPet pet) {
        return pet.getOwner() instanceof Player && !pet.getPendingGift().isEmpty();
    }

    /**
     * Whether the pet has another present in mind: its owner is about to be given it, its last
     * present was long enough ago, and there is something in its bag it may give.
     */
    public static boolean thinksOfAnother(AbstractPet pet) {
        return pet.getOwner() instanceof Player
            && !pet.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get())
            && !GiftPool.of(pet).isEmpty();
    }

    /** Whether the pet may set off now: it is carrying a present already, or it thinks of another. */
    public static boolean mayBegin(AbstractPet pet) {
        return carrying(pet) || thinksOfAnother(pet);
    }
}
