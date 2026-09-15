package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.google.common.collect.ImmutableMap;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

// Keeps the pet sitting.
public class SitBehavior<E extends AbstractPet> extends Behavior<E> {
    // No required memories.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of();

    /**
     * Task with a 100-tick timeout.
     */
    public SitBehavior() {
        super(REQUIRED_MEMORIES, 100);
    }

    /**
     * Checks whether the task can start.
     * @param level the server level
     * @param entity the pet entity
     * @return whether the task can start
     */
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        return entity.getPetDirective() == PetDirective.STAY;
    }
    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        entity.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    /**
     * Checks whether the task should continue.
     * @param level the server level
     * @param entity the pet entity
     * @param gameTime the current time
     * @return whether the task can continue
     */
    @Override
    protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
        return entity.getPetDirective() == PetDirective.STAY;
    }

    
}

