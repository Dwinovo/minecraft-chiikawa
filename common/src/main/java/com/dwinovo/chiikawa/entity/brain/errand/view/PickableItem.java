package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.init.InitTag;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * An item lying about that a pet may pick up, as it is now: which item it is, and where. What counts as
 * one is decided here and nowhere else, by the sensor that finds them and by whoever asks whether one
 * is still there.
 *
 * @param id the item entity's id, which is what an errand to fetch it keeps to
 * @param where where it lies
 */
public record PickableItem(UUID id, GlobalPos where) {
    /**
     * @param item an item entity
     * @return it as a pet's to pick up, or nothing if it is gone, still too new to be picked up by anyone,
     *         or not one of the items the pets pick up
     */
    public static Optional<PickableItem> of(ItemEntity item) {
        if (!item.isAlive() || item.hasPickUpDelay() || !item.getItem().is(InitTag.ENTITY_PICKABLE_ITEMS)) {
            return Optional.empty();
        }
        return Optional.of(new PickableItem(item.getUUID(), GlobalPos.of(item.level().dimension(), item.blockPosition())));
    }
}
