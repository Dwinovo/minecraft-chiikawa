package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
// Random wandering behavior.
public class RandomWalkTask extends Behavior<AbstractPet> {
    // Required memories.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
    );

    public RandomWalkTask() {
        super(REQUIRED_MEMORIES, 100);
    }
    /**
     * Start wandering.
     */
    @SuppressWarnings("null")
    @Override
    protected void start(ServerLevel level, AbstractPet entity, long gameTime) {
        BlockPos newTarget = getRandomTarget(entity);
        BehaviorUtils.setWalkAndLookTargetMemories(entity, newTarget, 0.6F, 0);
    }
    /**
     * Picks a random reachable target.
     * @param entity the pet entity
     * @return the target position
     */
    private BlockPos getRandomTarget(AbstractPet entity) {
        BlockPos currentPosition = entity.blockPosition();
        for (int i = 0; i < 10; i++) {
            int randomX = currentPosition.getX() + entity.getRandom().nextInt(9) - 4;
            int randomZ = currentPosition.getZ() + entity.getRandom().nextInt(9) - 4;
            BlockPos randomBlockPos = new BlockPos(randomX, currentPosition.getY(), randomZ);
            if (entity.getNavigation().isStableDestination(randomBlockPos)) {
                return randomBlockPos;
            }
        }
        return currentPosition;
    }

    
}

