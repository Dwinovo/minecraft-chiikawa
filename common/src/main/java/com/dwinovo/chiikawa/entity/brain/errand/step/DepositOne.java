package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.utils.Deliveries;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Puts one of the things the pets deliver from the pet's bag into the container, the first it finds: into the loader's
 * item storage where the container has one, and otherwise straight into a free slot or onto a stack with room. One
 * a tick is the pace the container fills at. Gives up if the container is gone, or does not take it: another thing in
 * the bag might have fitted, but the first is the one that is put in, and a pet that kept offering it would stand there
 * for good. Done in the tick it takes, so it is never cut half way: the thing is in the container or still in the bag.
 */
public final class DepositOne implements Step {
    private final BlockPos container;
    private final Deliveries deliveries;

    /**
     * @param container the container's block
     * @param deliveries which containers and which things the delivery is about
     */
    public DepositOne(BlockPos container, Deliveries deliveries) {
        this.container = container;
        this.deliveries = deliveries;
    }

    /**
     * @param container the container's block
     * @param deliveries which containers and which things the delivery is about
     * @return whether to go on putting things in: while the pet has something to deliver and the container would take it
     */
    public static Predicate<Handle> whileItemsFit(BlockPos container, Deliveries deliveries) {
        return handle -> {
            AbstractPet pet = LiveHandle.of(handle).pet();
            return pet.level() instanceof ServerLevel level && deliveries.worthVisiting(level, container, pet.getBackpack());
        };
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!(pet.level() instanceof ServerLevel level)) {
            return StepStatus.gaveUp("gone");
        }
        ItemStack source = deliveries.first(pet.getBackpack());
        if (source.isEmpty()) {
            return StepStatus.DONE;
        }
        if (Services.ITEM_TRANSFER.hasBlockStorage(level, container)) {
            ItemStack testStack = source.copy();
            testStack.setCount(1);
            int inserted = Services.ITEM_TRANSFER.insertIntoBlock(level, container, testStack, false);
            if (inserted > 0) {
                source.shrink(inserted);
                return StepStatus.DONE;
            }
        }
        if (!(level.getBlockEntity(container) instanceof Container target)) {
            return StepStatus.gaveUp("gone");
        }
        return putInto(target, source) ? StepStatus.DONE : StepStatus.gaveUp("no_room");
    }

    /** @return whether one of the stack was put in the container, and if so it is taken from the stack */
    private static boolean putInto(Container target, ItemStack source) {
        ItemStack remaining = source.copy();
        remaining.setCount(1);
        for (int slot = 0; slot < target.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = target.getItem(slot);
            if (slotStack.isEmpty()) {
                int maxSize = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                target.setItem(slot, remaining.split(maxSize));
            } else if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int transfer = Math.min(remaining.getCount(), space);
                slotStack.grow(transfer);
                remaining.shrink(transfer);
                target.setItem(slot, slotStack);
            }
        }
        if (remaining.getCount() >= 1) {
            return false;
        }
        source.shrink(1);
        target.setChanged();
        return true;
    }
}
