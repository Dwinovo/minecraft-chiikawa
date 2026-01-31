package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.utils.BlockSearch;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
// Finds nearby containers that can accept delivery items.
public class PetContainerSensor extends Sensor<AbstractPet> {
    // Search radius.
    private static final int MAX_RADIUS = 5;
    // Vertical search range.
    private static final int VERTICAL_RANGE = 1;
    /**
     * Memory types used by this sensor.
     * @return required memory types
     */
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            InitMemory.CONTAINER_POS.get()
        );
     }

    
    /**
     * Scan for reachable delivery containers.
     * @param level the server level
     * @param entity the pet entity
     */
    @Override
     protected void doTick(ServerLevel level, AbstractPet entity) {
         boolean hasDeliverItem = hasTaggedItem(entity.getBackpack());
         if (entity.getPetMode() != PetMode.WORK || entity.getPetJobId() != InitRegistry.FARMER_ID || !hasDeliverItem) {
            entity.getBrain().eraseMemory(InitMemory.CONTAINER_POS.get());
            return;
         }
         if (entity.getBrain().getMemory(InitMemory.PLANT_POS.get()).isPresent()
            || entity.getBrain().getMemory(InitMemory.HARVEST_POS.get()).isPresent()) {
            entity.getBrain().eraseMemory(InitMemory.CONTAINER_POS.get());
            return;
         }
         if (entity.getBrain().getMemory(InitMemory.CONTAINER_POS.get()).isPresent()) {
            net.minecraft.core.BlockPos current = entity.getBrain().getMemory(InitMemory.CONTAINER_POS.get()).get();
            if (level.getBlockState(current).is(InitTag.ENTITY_DELEVER_CONTAINER)
                && Utils.canReach(entity, current)
                && canInsertContainer(level, current, entity)) {
                return;
            }
            entity.getBrain().eraseMemory(InitMemory.CONTAINER_POS.get());
         }
         if (entity.getPetMode() == PetMode.WORK && entity.getPetJobId() == InitRegistry.FARMER_ID && hasDeliverItem) {
            // Spiral search for reachable containers with free space.
            BlockSearch.spiralBlockSearch(level, entity, MAX_RADIUS, VERTICAL_RANGE,
            (lvl, pos, pet) -> 
                Utils.canReach(pet, pos)
                && lvl.getBlockState(pos).is(InitTag.ENTITY_DELEVER_CONTAINER)
                && hasDeliverItem
                && canInsertContainer(lvl, pos, pet)
            ).ifPresentOrElse(foundPos -> {
                entity.getBrain().setMemory(InitMemory.CONTAINER_POS.get(), foundPos);
            }, () -> {
                entity.getBrain().eraseMemory(InitMemory.CONTAINER_POS.get());
            });
        }
    }

    private static boolean canInsertContainer(ServerLevel level, net.minecraft.core.BlockPos pos, AbstractPet pet) {
        if (Services.ITEM_TRANSFER.hasBlockStorage(level, pos)) {
            net.minecraft.world.Container backpack = pet.getBackpack();
            for (int i = 0; i < backpack.getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = backpack.getItem(i);
                if (stack.isEmpty() || !stack.is(InitTag.ENTITY_DELIVER_ITEMS)) {
                    continue;
                }
                net.minecraft.world.item.ItemStack testStack = stack.copy();
                testStack.setCount(1);
                int inserted = Services.ITEM_TRANSFER.insertIntoBlock(level, pos, testStack, true);
                if (inserted > 0) {
                    return true;
                }
            }
            return false;
        }
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof net.minecraft.world.Container container)) {
            return false;
        }
        net.minecraft.world.Container backpack = pet.getBackpack();
        for (int i = 0; i < backpack.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = backpack.getItem(i);
            if (stack.isEmpty() || !stack.is(InitTag.ENTITY_DELIVER_ITEMS)) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                net.minecraft.world.item.ItemStack slotStack = container.getItem(slot);
                if (slotStack.isEmpty()) {
                    return true;
                }
                if (net.minecraft.world.item.ItemStack.isSameItemSameTags(slotStack, stack)
                    && slotStack.getCount() < slotStack.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasTaggedItem(net.minecraft.world.Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.is(InitTag.ENTITY_DELIVER_ITEMS)) {
                return true;
            }
        }
        return false;
    }
}

