package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

/** Keeps walking toward the owner and looking at them. */
public class FollowOwnerBehavior extends Behavior<AbstractPet> {
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
    );
    private static final float SPEED = 1.0F;

    public FollowOwnerBehavior() {
        // Runs for as long as its activity does; the follow_owner intent ends it.
        super(REQUIRED_MEMORIES, Integer.MAX_VALUE);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return pet.getOwner() != null;
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        walkToOwner(pet);
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        walkToOwner(pet);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return pet.getOwner() != null;
    }

    private static void walkToOwner(AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        if (owner != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(pet, owner, SPEED, AnchorDistances.FOLLOW_ARRIVE);
        }
    }
}
