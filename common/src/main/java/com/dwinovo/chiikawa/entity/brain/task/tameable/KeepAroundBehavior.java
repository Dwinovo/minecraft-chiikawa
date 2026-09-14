package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

// Keeps the pet near its owner or home.
public class KeepAroundBehavior<E extends AbstractPet> extends Behavior<E> {
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of(
        MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
        MemoryModuleType.HOME, MemoryStatus.REGISTERED
    );

    // Distance to start following the owner.
    private final float followMasterDistance;
    // Distance to start returning to home.
    private final float keepHomeAroundDistance;
    // Distance to trigger teleport.
    private final float teleportDistance;

    public KeepAroundBehavior(float followMasterDistance, float keepHomeAroundDistance, float teleportDistance) {
        super(REQUIRED_MEMORIES, 15);
        this.followMasterDistance = followMasterDistance;
        this.keepHomeAroundDistance = keepHomeAroundDistance;
        this.teleportDistance = teleportDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E pet) {
        if (pet.isLeashed() || pet.isPassenger()) {
            return false;
        }

        Optional<LivingEntity> owner = Optional.ofNullable(pet.getOwner());
        if (pet.getPetMode() == PetMode.FOLLOW) {
            return owner.filter(o -> !o.isSpectator() && isSameLevel(level, o) && pet.distanceTo(o) >= this.followMasterDistance)
                .isPresent();
        }

        if (pet.getPetMode() == PetMode.WORK) {
            return pet.getBrain().getMemory(MemoryModuleType.HOME)
                .filter(home -> isSameDimension(level, home) && home.pos().distManhattan(pet.getOnPos()) > this.keepHomeAroundDistance)
                .isPresent();
        }

        return false;
    }

    @Override
    protected void start(ServerLevel level, E pet, long time) {
        Optional<LivingEntity> owner = Optional.ofNullable(pet.getOwner());
        if (pet.getPetMode() == PetMode.FOLLOW) {
            owner.filter(o -> isSameLevel(level, o)).ifPresent(o -> {
                if (pet.distanceTo(o) > this.teleportDistance) {
                    pet.teleportToOwner(level, o);
                }
                else {
                    BehaviorUtils.setWalkAndLookTargetMemories(pet, o, 1.0F, 2);
                }
            });
        }
        else if (pet.getPetMode() == PetMode.WORK) {
            pet.getBrain().getMemory(MemoryModuleType.HOME)
                .filter(home -> isSameDimension(level, home))
                .ifPresent(home -> pet.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(home.pos(), 0.6F, 0)
                ));
        }
    }

    private boolean isSameDimension(ServerLevel level, GlobalPos home) {
        return home.dimension().equals(level.dimension());
    }

    // Never follow (or teleport to) an owner in another dimension.
    private static boolean isSameLevel(ServerLevel level, LivingEntity owner) {
        return owner.level() == level;
    }
}
