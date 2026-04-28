package com.dwinovo.chiikawa.entity.brain.task.farmer;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.platform.Services;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

// Delivers tagged items to a nearby container.
public class DeliverCropBehavior extends Behavior<AbstractPet> {
    private boolean didOpen;
    private BlockPos openContainerPos;
    // Required memories.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        InitMemory.CONTAINER_POS.get(), MemoryStatus.VALUE_PRESENT
    );

    /**
     * Task with a long timeout for delivery.
     */
    public DeliverCropBehavior() {
        super(REQUIRED_MEMORIES, 3000);
    }

    /**
     * Checks whether delivery can start.
     * @param world the server level
     * @param pet the pet entity
     * @return whether the task can start
     */
    @SuppressWarnings("null")
    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, AbstractPet pet) {
        if (pet.getPetMode() != PetMode.WORK || pet.getPetJobId() != InitRegistry.FARMER_ID) {
            return false;
        }
        if (pet.getBrain().getMemory(InitMemory.PLANT_POS.get()).isPresent()
            || pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).isPresent()) {
            return false;
        }
        Optional<BlockPos> containerPosOpt = pet.getBrain().getMemory(InitMemory.CONTAINER_POS.get());
        if (containerPosOpt.isEmpty()) {
            return false;
        }
        BlockPos containerPos = containerPosOpt.get();
        if (!world.getBlockState(containerPos).is(InitTag.ENTITY_DELEVER_CONTAINER)) {
            return false;
        }
        if (!hasTaggedItem(pet.getBackpack(), InitTag.ENTITY_DELIVER_ITEMS)) {
            return false;
        }
        if (!canInsertContainer(world, containerPos, pet.getBackpack(), InitTag.ENTITY_DELIVER_ITEMS)) {
            return false;
        }
        return pet.distanceToSqr(Vec3.atCenterOf(containerPos)) <= 9.0D;
    }

    /**
     * Starts the delivery and opens the container if needed.
     * @param world the server level
     * @param pet the pet entity
     * @param time the current time
     */
    @SuppressWarnings("null")
    @Override
    protected void start(ServerLevel world, AbstractPet pet, long time) {
        try {
            this.didOpen = true;
            Optional<BlockPos> containerPosOpt = pet.getBrain().getMemory(InitMemory.CONTAINER_POS.get());
            if (containerPosOpt.isPresent()) {
                BlockPos containerPos = containerPosOpt.get();
                this.openContainerPos = containerPos;
                BlockEntity blockEntity = world.getBlockEntity(containerPos);
                if (blockEntity instanceof ChestBlockEntity) {
                    pet.triggerAnim("use_mainhand");
                    world.playSound(null, containerPos.getX(), containerPos.getY(), containerPos.getZ(),
                        SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                    world.blockEvent(containerPos, world.getBlockState(containerPos).getBlock(), 1, 1);
                }
            } else {
                this.openContainerPos = null;
            }
        } catch (Exception e) {
            // Ignore and allow task to fail silently.
        }
    }

    /**
     * Checks whether delivery should continue.
     * @param world the server level
     * @param pet the pet entity
     * @param time the current time
     * @return whether the task can continue
     */
    @SuppressWarnings("null")
    @Override
    protected boolean canStillUse(ServerLevel world, AbstractPet pet, long time) {
        if (pet.getPetMode() != PetMode.WORK || pet.getPetJobId() != InitRegistry.FARMER_ID) {
            return false;
        }
        if (pet.getBrain().getMemory(InitMemory.PLANT_POS.get()).isPresent()
            || pet.getBrain().getMemory(InitMemory.HARVEST_POS.get()).isPresent()) {
            return false;
        }
        Optional<BlockPos> containerPosOpt = pet.getBrain().getMemory(InitMemory.CONTAINER_POS.get());
        if (containerPosOpt.isEmpty()) {
            return false;
        }
        BlockPos containerPos = containerPosOpt.get();
        if (!world.getBlockState(containerPos).is(InitTag.ENTITY_DELEVER_CONTAINER)) {
            return false;
        }
        if (pet.distanceToSqr(Vec3.atCenterOf(containerPos)) > 9.0D) {
            return false;
        }
        return hasTaggedItem(pet.getBackpack(), InitTag.ENTITY_DELIVER_ITEMS)
            && canInsertContainer(world, containerPos, pet.getBackpack(), InitTag.ENTITY_DELIVER_ITEMS);
    }

    /**
     * Per-tick delivery work.
     * @param world the server level
     * @param pet the pet entity
     * @param time the current time
     */
    @SuppressWarnings("null")
    @Override
    protected void tick(ServerLevel world, AbstractPet pet, long time) {
        pet.getNavigation().stop();
        Optional<BlockPos> containerPosOpt = pet.getBrain().getMemory(InitMemory.CONTAINER_POS.get());
        if (containerPosOpt.isEmpty()) {
            return;
        }
        BlockPos containerPos = containerPosOpt.get();
        ItemStack sourceStack = findFirstTaggedItem(pet.getBackpack(), InitTag.ENTITY_DELIVER_ITEMS);
        if (sourceStack.isEmpty()) {
            closeIfFinished(world, pet, containerPos);
            return;
        }

        if (Services.ITEM_TRANSFER.hasBlockStorage(world, containerPos)) {
            ItemStack testStack = sourceStack.copy();
            testStack.setCount(1);
            int inserted = Services.ITEM_TRANSFER.insertIntoBlock(world, containerPos, testStack, false);
            if (inserted > 0) {
                sourceStack.shrink(inserted);
                closeIfFinished(world, pet, containerPos);
                return;
            }
        }

        BlockEntity blockEntity = world.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return;
        }

        ItemStack remaining = sourceStack.copy();
        remaining.setCount(1);
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = container.getItem(slot);
            if (slotStack.isEmpty()) {
                int maxSize = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                container.setItem(slot, remaining.split(maxSize));
            } else if (ItemStack.isSameItemSameTags(slotStack, remaining)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int transfer = Math.min(remaining.getCount(), space);
                slotStack.grow(transfer);
                remaining.shrink(transfer);
                container.setItem(slot, slotStack);
            }
        }

        if (remaining.getCount() < 1) {
            sourceStack.shrink(1);
            container.setChanged();
            closeIfFinished(world, pet, containerPos);
        }
    }

    /**
     * Cleanup when the task stops.
     * @param world the server level
     * @param pet the pet entity
     * @param time the current time
     */
    @SuppressWarnings("null")
    @Override
    protected void stop(ServerLevel world, AbstractPet pet, long time) {
        super.stop(world, pet, time);
        Optional<BlockPos> containerPosOpt = pet.getBrain().getMemory(InitMemory.CONTAINER_POS.get());
        if (this.didOpen) {
            BlockPos containerPos = this.openContainerPos;
            if (containerPos == null && containerPosOpt.isPresent()) {
                containerPos = containerPosOpt.get();
            }
            if (containerPos != null) {
                closeChest(world, containerPos);
            }
        }
        this.didOpen = false;
        this.openContainerPos = null;
    }

    private static boolean hasTaggedItem(Container container, TagKey<Item> tag) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack findFirstTaggedItem(Container container, TagKey<Item> tag) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.is(tag)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean canInsertContainer(ServerLevel level, BlockPos pos, Container source, TagKey<Item> tag) {
        if (Services.ITEM_TRANSFER.hasBlockStorage(level, pos)) {
            for (int i = 0; i < source.getContainerSize(); i++) {
                ItemStack stack = source.getItem(i);
                if (stack.isEmpty() || !stack.is(tag)) {
                    continue;
                }
                ItemStack testStack = stack.copy();
                testStack.setCount(1);
                int inserted = Services.ITEM_TRANSFER.insertIntoBlock(level, pos, testStack, true);
                if (inserted > 0) {
                    return true;
                }
            }
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }
        for (int i = 0; i < source.getContainerSize(); i++) {
            ItemStack stack = source.getItem(i);
            if (stack.isEmpty() || !stack.is(tag)) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack slotStack = container.getItem(slot);
                if (slotStack.isEmpty()) {
                    return true;
                }
                if (ItemStack.isSameItemSameTags(slotStack, stack)
                    && slotStack.getCount() < slotStack.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void closeChest(ServerLevel world, BlockPos containerPos) {
        BlockEntity blockEntity = world.getBlockEntity(containerPos);
        if (blockEntity instanceof ChestBlockEntity) {
            world.playSound(null, containerPos.getX(), containerPos.getY(), containerPos.getZ(),
                SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.blockEvent(containerPos, world.getBlockState(containerPos).getBlock(), 1, 0);
        }
    }

    private void closeIfFinished(ServerLevel world, AbstractPet pet, BlockPos containerPos) {
        boolean hasItems = hasTaggedItem(pet.getBackpack(), InitTag.ENTITY_DELIVER_ITEMS);
        if (hasItems && canInsertContainer(world, containerPos, pet.getBackpack(), InitTag.ENTITY_DELIVER_ITEMS)) {
            return;
        }
        pet.getBrain().eraseMemory(InitMemory.CONTAINER_POS.get());
        closeChest(world, containerPos);
        this.didOpen = false;
        this.openContainerPos = null;
    }
}

