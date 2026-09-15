package com.dwinovo.chiikawa.mixin;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes which behaviors a brain registered under each activity, so
 * {@link com.dwinovo.chiikawa.entity.brain.PetActivities} can stop the behaviors of
 * an activity it switches away from without keeping a second copy of that mapping.
 */
@Mixin(Brain.class)
public interface BrainAccessor {
    @Accessor("availableBehaviorsByPriority")
    Map<Integer, Map<Activity, Set<BehaviorControl<?>>>> chiikawa$getAvailableBehaviorsByPriority();
}
