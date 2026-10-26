package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.gifting.GiftOccasion;
import com.dwinovo.chiikawa.init.InitTag;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Picks up an item that is within reach, into the pet's bag. Done in the tick it takes: the item is in the bag,
 * or the pet gives up because it is gone, is no longer one the pets pick up, or does not fit. It does not look
 * how near the pet is; the walk before it is what brings the pet within reach. A little thing it is glad to have found,
 * a flower, may put a present in its mind, and is what it means to give.
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
            .filter(PickableItem::isForPets);
        if (target.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        AbstractPet pet = live.pet();
        ItemEntity found = target.get();
        ItemStack stack = found.getItem();
        int inserted = PickableItem.store(pet, stack, false);
        if (inserted <= 0) {
            return StepStatus.gaveUp("no_room");
        }
        handle.action(PetAction.PICKUP);
        pet.take(found, inserted);
        if (stack.is(InitTag.GIFT_FINDS)) {
            PetMind.post(pet, new Message.Occasion(GiftOccasion.FOUND_SOMETHING, Optional.of(stack.copyWithCount(1))));
        }
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
}
