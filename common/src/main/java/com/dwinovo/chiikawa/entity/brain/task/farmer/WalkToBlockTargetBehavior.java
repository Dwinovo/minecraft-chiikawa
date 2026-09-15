package com.dwinovo.chiikawa.entity.brain.task.farmer;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableMap;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

// Walks to the block a work-target memory points at, such as a crop or a weed.
public class WalkToBlockTargetBehavior extends Behavior<AbstractPet> {
    private final MemoryModuleType<BlockPos> target;
    private final BiPredicate<ServerLevel, BlockPos> qualifies;
    // Move speed.
    private final float speed;

    /**
     * Creates the task.
     * @param target the memory holding the block to walk to
     * @param qualifies whether the block there is still worth walking to
     * @param speed move speed
     */
    public WalkToBlockTargetBehavior(MemoryModuleType<BlockPos> target, BiPredicate<ServerLevel, BlockPos> qualifies, float speed) {
        super(ImmutableMap.of(target, MemoryStatus.VALUE_PRESENT), 15);
        this.target = target;
        this.qualifies = qualifies;
        this.speed = speed;
    }

    /**
     * Checks whether the task can start. A target that no longer qualifies is forgotten;
     * one that cannot be reached is also blacklisted so the sensor moves on to another.
     * @param world the server level
     * @param pet the pet entity
     * @return whether the task can start
     */
    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, AbstractPet pet) {
        BlockPos pos = pet.getBrain().getMemory(target).orElseThrow();
        if (qualifies.test(world, pos)) {
            // Single reachability pathfind for the chosen target (the sensor does not
            // check reach per candidate).
            if (Utils.canReach(pet, pos)) {
                return true;
            }
            pet.blacklistUnreachable(pos);
        }
        pet.getBrain().eraseMemory(target);
        return false;
    }

    /**
     * Start walking to the target.
     * @param world the server level
     * @param pet the pet entity
     * @param time the current time
     */
    @Override
    protected void start(ServerLevel world, AbstractPet pet, long time) {
        BehaviorUtils.setWalkAndLookTargetMemories(pet, pet.getBrain().getMemory(target).orElseThrow(), speed, 0);
    }
}
