package com.dwinovo.chiikawa.utils;

import com.dwinovo.chiikawa.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What a delivery asks of the world: which blocks it puts things in, which of the pet's things it puts there, and
 * whether they would fit. The one place that says so, for the sensor that finds the chest, the view that checks it
 * still holds and the steps that fill it; which blocks and which things is the delivery's to name.
 *
 * @param containers the blocks the pets deliver to
 * @param items the things in the bag that the pets deliver
 */
public record Deliveries(TagKey<Block> containers, TagKey<Item> items) {
    /** @return whether the block is one the pets deliver to */
    public boolean isContainer(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(containers);
    }

    /** @return whether the bag holds anything the pets deliver */
    public boolean hasItems(Container bag) {
        return !first(bag).isEmpty();
    }

    /** @return the first stack in the bag that the pets deliver, the stack itself and not a copy, or empty */
    public ItemStack first(Container bag) {
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack stack = bag.getItem(i);
            if (!stack.isEmpty() && stack.is(items)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * @param bag where the things to deliver are
     * @return whether the container at the position would take one of them: a container the loader gives item
     *         storage for is asked, and any other is looked through for a free slot or a stack with room
     */
    public boolean canTake(ServerLevel level, BlockPos pos, Container bag) {
        if (Services.ITEM_TRANSFER.hasBlockStorage(level, pos)) {
            for (int i = 0; i < bag.getContainerSize(); i++) {
                ItemStack stack = bag.getItem(i);
                if (stack.isEmpty() || !stack.is(items)) {
                    continue;
                }
                ItemStack testStack = stack.copy();
                testStack.setCount(1);
                if (Services.ITEM_TRANSFER.insertIntoBlock(level, pos, testStack, true) > 0) {
                    return true;
                }
            }
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack stack = bag.getItem(i);
            if (stack.isEmpty() || !stack.is(items)) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack slotStack = container.getItem(slot);
                if (slotStack.isEmpty()) {
                    return true;
                }
                if (ItemStack.isSameItemSameComponents(slotStack, stack)
                    && slotStack.getCount() < slotStack.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return whether a container at the position is worth walking to with this bag: one the pets deliver to, that
     *         would take something from it
     */
    public boolean worthVisiting(ServerLevel level, BlockPos pos, Container bag) {
        return isContainer(level, pos) && hasItems(bag) && canTake(level, pos, bag);
    }
}
