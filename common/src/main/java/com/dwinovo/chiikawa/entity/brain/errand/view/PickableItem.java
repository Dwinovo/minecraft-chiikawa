package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitTag;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * An item lying about that a pet may pick up, as it is now: which item it is, and where. What counts as
 * one is decided here and nowhere else, by the sensor that finds them, by whoever asks whether one is
 * still there, and by the step that takes it: one the pets pick up, not too new to be picked up by
 * anyone, and with room for it in the pet's bag. A pet is not offered what it has no room for.
 *
 * @param id the item entity's id, which is what an errand to fetch it keeps to
 * @param where where it lies
 */
public record PickableItem(UUID id, GlobalPos where) {
    /**
     * @param pet who would pick it up
     * @param item an item entity
     * @return it as that pet's to pick up, or nothing if it is gone, still too new to be picked up by anyone,
     *         not one of the items the pets pick up, or more than the pet's bag has room for
     */
    public static Optional<PickableItem> of(AbstractPet pet, ItemEntity item) {
        if (!isForPets(item) || store(pet, item.getItem(), true) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new PickableItem(item.getUUID(), GlobalPos.of(item.level().dimension(), item.blockPosition())));
    }

    /** @return whether the item is there to be picked up and is of the kind the pets pick up, whatever the room in the bag */
    public static boolean isForPets(ItemEntity item) {
        return item.isAlive() && !item.hasPickUpDelay() && item.getItem().is(InitTag.ENTITY_PICKABLE_ITEMS);
    }

    /**
     * Puts the stack away in the pet's pockets as far as it fits.
     *
     * @param simulate whether to only find out how much would fit
     * @return how many of the stack went in, or would
     */
    public static int store(AbstractPet pet, ItemStack stack, boolean simulate) {
        if (simulate) {
            return pet.pockets().roomFor(stack);
        }
        return stack.getCount() - pet.pockets().stow(stack).getCount();
    }
}
