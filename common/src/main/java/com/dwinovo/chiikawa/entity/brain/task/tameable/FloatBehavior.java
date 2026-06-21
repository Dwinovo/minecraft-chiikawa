package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.google.common.collect.ImmutableMap;
import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

/**
 * Brain-native equivalent of vanilla {@code FloatGoal}: while the pet is
 * submerged (or in lava) it repeatedly requests a jump so it bobs up to the
 * surface and keeps breathing instead of sinking and drowning. Kept in the
 * Brain so all pet AI lives in one paradigm — there is no {@code FloatGoal}
 * on the goal selector. Runs in the CORE activity at top priority; it only
 * touches the jump control, so it never fights the brain's move control.
 */
public class FloatBehavior<E extends AbstractPet> extends Behavior<E> {
    // No required memories: floating must work regardless of mode/job state.
    private static final Map<MemoryModuleType<?>, MemoryStatus> REQUIRED_MEMORIES = ImmutableMap.of();

    public FloatBehavior() {
        // Max run time is effectively unbounded; canStillUse governs the lifecycle.
        super(REQUIRED_MEMORIES, Integer.MAX_VALUE);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        return entity.isInWater() && entity.getFluidHeight(FluidTags.WATER) > entity.getFluidJumpThreshold()
            || entity.isInLava();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
        return checkExtraStartConditions(level, entity);
    }

    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        // Same 80% cadence as vanilla FloatGoal: enough to stay at the surface
        // without locking the jump every single tick.
        if (entity.getRandom().nextFloat() < 0.8F) {
            entity.getJumpControl().jump();
        }
    }
}
