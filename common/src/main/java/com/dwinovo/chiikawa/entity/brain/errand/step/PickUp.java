package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.platform.Services;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Picks up an item that is within reach, into the pet's bag. Done in the tick it takes: the item is in the bag,
 * or the pet gives up because it is gone, is no longer one the pets pick up, or does not fit. It does not look
 * how near the pet is; the walk before it is what brings the pet within reach.
 *
 * <p>An action of one tick, so it is never cut half way: an errand asked to end waits for the next tick at most,
 * and ends either before the item is taken or after.
 */
public final class PickUp implements Step {
    private final UUID item;

    /** @param item the id of the item entity */
    public PickUp(UUID item) {
        this.item = item;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<ItemEntity> target = live.creature(item).filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast)
            .filter(found -> PickableItem.of(found).isPresent());
        if (target.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        AbstractPet pet = live.pet();
        ItemEntity found = target.get();
        ItemStack stack = found.getItem();
        int inserted = Services.ITEM_TRANSFER.insertIntoEntity(pet, stack, false);
        if (inserted == 0) {
            inserted = insertIntoContainer(pet.getBackpack(), stack);
        }
        if (inserted <= 0) {
            return StepStatus.gaveUp("no_room");
        }
        handle.action(PetAction.PICKUP);
        pet.take(found, inserted);
        if (stack.getCount() - inserted <= 0) {
            found.discard();
        } else {
            ItemStack remaining = stack.copy();
            remaining.shrink(inserted);
            found.setItem(remaining);
        }
        pet.onItemPickup(found);
        return StepStatus.DONE;
    }

    /** @return how many of the stack went into the container, filling it slot by slot in order */
    private static int insertIntoContainer(Container container, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = container.getItem(slot);
            if (slotStack.isEmpty()) {
                int maxSize = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                container.setItem(slot, remaining.split(maxSize));
            } else if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int transfer = Math.min(remaining.getCount(), space);
                slotStack.grow(transfer);
                remaining.shrink(transfer);
                container.setItem(slot, slotStack);
            }
        }
        return stack.getCount() - remaining.getCount();
    }
}
