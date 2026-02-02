package com.dwinovo.chiikawa.entity.brain.task.tameable;

import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import com.dwinovo.chiikawa.platform.Services;
// Picks up nearby items.
public class PickUpItemTask extends Behavior<AbstractPet> {
    // Move speed.
    private final float speedModifier;
    private static final double PICKUP_DISTANCE_SQ = 2.25D;
    // Required memories.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        InitMemory.PICKABLE_ITEM.get(), MemoryStatus.VALUE_PRESENT
    );
    /**
     * Creates the task.
     * @param speedModifier move speed
     */
    public PickUpItemTask(float speedModifier) {
        super(REQUIRED_MEMORIES, 10);
        this.speedModifier = speedModifier;
    }
    /**
     * Checks whether the task can start.
     * @param level the server level
     * @param entity the pet entity
     * @return whether the task can start
     */
    @SuppressWarnings("null")
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet entity) {
        return entity.isTame() && entity.getPetMode() == PetMode.WORK;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet entity, long time) {
        return entity.isTame()
            && entity.getPetMode() == PetMode.WORK
            && entity.getBrain().getMemory(InitMemory.PICKABLE_ITEM.get()).isPresent();
    }
    /**
     * Start behavior.
     * @param level the server level
     * @param entity the pet entity
     * @param time the current time
     */
    @SuppressWarnings("null")
    @Override
    protected void start(ServerLevel level, AbstractPet entity, long time) {
        ItemEntity target = entity.getBrain().getMemory(InitMemory.PICKABLE_ITEM.get()).orElse(null);
        if (target != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(entity, target, speedModifier, 0);
        }
        else{
            entity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }        
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet entity, long time) {
        ItemEntity target = entity.getBrain().getMemory(InitMemory.PICKABLE_ITEM.get()).orElse(null);
        if (target == null) {
            entity.getBrain().eraseMemory(InitMemory.PICKABLE_ITEM.get());
            return;
        }
        if (!target.isAlive() || target.hasPickUpDelay() || !target.getItem().is(InitTag.ENTITY_PICKABLE_ITEMS)) {
            entity.getBrain().eraseMemory(InitMemory.PICKABLE_ITEM.get());
            return;
        }
        if (entity.distanceToSqr(target) > PICKUP_DISTANCE_SQ) {
            return;
        }
        ItemStack stack = target.getItem();
        int inserted = Services.ITEM_TRANSFER.insertIntoEntity(entity, stack, false);
        int pickedCount = inserted;
        if (pickedCount <= 0) {
            return;
        }
        entity.take(target, pickedCount);
        if (stack.getCount() - pickedCount <= 0) {
            target.discard();
        } else {
            ItemStack remaining = stack.copy();
            remaining.shrink(pickedCount);
            target.setItem(remaining);
        }
        entity.onItemPickup(target);
        entity.getBrain().eraseMemory(InitMemory.PICKABLE_ITEM.get());
    }

}
