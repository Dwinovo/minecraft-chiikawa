package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.gifting.GiftChoice;
import net.minecraft.world.item.ItemStack;

/**
 * Takes a present out of the pet's bag into its paws: one of the thing it chose, and the present it had in mind is
 * in its paws and no longer in its mind. Gives up if the thing is not in the bag after all, and then the pet still has
 * the present in mind. Done in the tick it takes, so it is never cut half way.
 *
 * <p>The present is the pet's to carry until {@code Presents} gives it, or {@link #putBack} returns it: those and
 * this step are the only writers of what is in the pet's paws, and this step the only one that consumes the present
 * the pet has in mind.
 */
public final class TakePresent implements Step {
    private final GiftChoice.Pick pick;

    /** @param pick what the pet chose to give, and where it has it */
    public TakePresent(GiftChoice.Pick pick) {
        this.pick = pick;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!pet.getBackpack().getItem(pick.slot()).is(pick.item())) {
            return StepStatus.gaveUp("no_present");
        }
        pet.setPendingGift(pet.getBackpack().removeItem(pick.slot(), 1));
        pet.giftIntentions().consume();
        return StepStatus.DONE;
    }

    /**
     * What is in the paws goes back into the bag, or on the ground if the bag is full, when the errand is off
     * before it was handed over: the owner is gone, or the pet was told to sit. A present that was handed over
     * is not in the paws, and nothing is done. Safe to call at any time, whether or not the pet took one.
     */
    public static void putBack(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (pet.getPendingGift().isEmpty()) {
            return;
        }
        ItemStack left = pet.getBackpack().addItem(pet.getPendingGift());
        if (!left.isEmpty()) {
            pet.spawnAtLocation(left);
        }
        pet.setPendingGift(ItemStack.EMPTY);
    }
}
