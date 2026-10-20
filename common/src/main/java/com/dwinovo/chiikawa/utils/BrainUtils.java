package com.dwinovo.chiikawa.utils;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.task.tameable.FloatBehavior;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.schedule.Activity;

/** Activities shared by every pet, whatever its job. */
public final class BrainUtils {
    private BrainUtils() {
    }

    /**
     * Always active and unrelated to any errand: float, and carry out the walk and the gaze the executor sets. The
     * brain's default activity is vanilla's {@code IDLE} with no behavior at all, so a pet does nothing by itself.
     */
    public static void addCoreTasks(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> floatInWater = Pair.of(0, new FloatBehavior<>());
        Pair<Integer, BehaviorControl<? super AbstractPet>> look = Pair.of(0, new LookAtTargetSink(45, 45));
        Pair<Integer, BehaviorControl<? super AbstractPet>> walkToTarget = Pair.of(1, new MoveToTargetSink());
        PetActivities.register(brain, Activity.CORE, ImmutableList.of(floatInWater, look, walkToTarget), Set.of());
    }
}
