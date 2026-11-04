package com.dwinovo.chiikawa.entity;

import java.util.function.BooleanSupplier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Where a pet puts away what it is given or finds. The backpack is one list of slots, but only some of them are room: slot
 * 0 is what the pet holds and slot 1 the bag it wears, and a thing put into either would change its job or take its bag's
 * place. So the room is the pet's own pockets, and the bag's ten while it wears a bag; {@link SimpleContainer#addItem}, which
 * fills any empty slot in order, is never used on a pet's backpack, this is.
 */
public final class PetPockets {
    private final SimpleContainer backpack;
    private final BooleanSupplier wearingBag;

    /**
     * @param backpack the pet's backpack, the hand and the bag's slot in it included
     * @param wearingBag whether the bag's own slots are there to use
     */
    public PetPockets(SimpleContainer backpack, BooleanSupplier wearingBag) {
        this.backpack = backpack;
        this.wearingBag = wearingBag;
    }

    /**
     * Puts the stack away as {@link SimpleContainer#addItem} does: into stacks of the same item first, then into empty slots.
     *
     * @param stack what to put away; left as it is
     * @return what did not fit, empty if all of it did
     */
    public ItemStack stow(ItemStack stack) {
        ItemStack left = stack.copy();
        int end = end();
        for (int slot = start(); slot < end && !left.isEmpty(); slot++) {
            ItemStack held = backpack.getItem(slot);
            if (!held.isEmpty() && ItemStack.isSameItemSameTags(held, left)) {
                int moved = Math.min(left.getCount(), capacityOf(held) - held.getCount());
                if (moved > 0) {
                    held.grow(moved);
                    left.shrink(moved);
                    backpack.setChanged();
                }
            }
        }
        for (int slot = start(); slot < end && !left.isEmpty(); slot++) {
            if (backpack.getItem(slot).isEmpty()) {
                int moved = Math.min(left.getCount(), capacityOf(left));
                backpack.setItem(slot, left.split(moved));
            }
        }
        return left;
    }

    /**
     * @param stack something to put away
     * @return how many of it {@link #stow} would take, up to all of it
     */
    public int roomFor(ItemStack stack) {
        int room = 0;
        for (int slot = start(); slot < end() && room < stack.getCount(); slot++) {
            ItemStack held = backpack.getItem(slot);
            if (held.isEmpty()) {
                room += capacityOf(stack);
            } else if (ItemStack.isSameItemSameTags(held, stack)) {
                room += capacityOf(held) - held.getCount();
            }
        }
        return Math.min(room, stack.getCount());
    }

    /** How much of this kind a slot holds, full. */
    private int capacityOf(ItemStack stack) {
        return Math.min(backpack.getMaxStackSize(), stack.getMaxStackSize());
    }

    private static int start() {
        return AbstractPet.BAG_SLOT + 1;
    }

    private int end() {
        return wearingBag.getAsBoolean() ? AbstractPet.FULL_BACKPACK_SIZE : AbstractPet.BACKPACK_SIZE;
    }
}
