package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.gifting.GiftPool;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Takes a present out of the pet's bag into its paws: one of the thing, drawn from what the pet may give by how
 * much it fancies giving each. A pet that is already carrying one, from before it was saved or from the shop, keeps
 * that and takes none. Gives up if there is nothing it may give after all. Done in the tick it takes, so it is never
 * cut half way.
 *
 * <p>The present is the pet's to carry until {@link HandOver} gives it, or {@link #putBack} returns it: those and
 * this step are the only writers of what is in the pet's paws.
 */
public final class TakePresent implements Step {
    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!pet.getPendingGift().isEmpty()) {
            return StepStatus.DONE;
        }
        Optional<GiftPool.Candidate> chosen = GiftPool.draw(GiftPool.of(pet), pet.getRandom());
        if (chosen.isEmpty()) {
            return StepStatus.gaveUp("no_present");
        }
        pet.setPendingGift(pet.getBackpack().removeItem(chosen.get().slot(), 1));
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
