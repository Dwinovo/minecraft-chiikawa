package com.dwinovo.chiikawa.utils;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.task.tameable.KeepAroundBehavior;
import com.dwinovo.chiikawa.entity.brain.task.tameable.PickUpItemTask;
import com.dwinovo.chiikawa.entity.brain.task.tameable.RandomWalkTask;
import com.dwinovo.chiikawa.entity.brain.task.tameable.SitBehavior;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public final class BrainUtils {
    private BrainUtils() {
    }

    public static void addCoreTasks(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> sit = Pair.of(0, new SitBehavior<>());
        Pair<Integer, BehaviorControl<? super AbstractPet>> keepAround = Pair.of(2, new KeepAroundBehavior<>(7, 13, 20));
        Pair<Integer, BehaviorControl<? super AbstractPet>> walkToTarget = Pair.of(1, new MoveToTargetSink());
        Pair<Integer, BehaviorControl<? super AbstractPet>> look = Pair.of(0, new LookAtTargetSink(45, 45));
        Pair<Integer, BehaviorControl<? super AbstractPet>> pickitem = Pair.of(3, new PickUpItemTask(0.7f));
        addActivity(brain, Activity.CORE, ImmutableList.of(look, sit, keepAround, walkToTarget,pickitem));
    }

    public static void addIdleTasks(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> randomTask = Pair.of(99, getLookAndRandomWalk());
        addActivity(brain, Activity.IDLE, ImmutableList.of(randomTask));
    }

    public static void addActivity(
        Brain<AbstractPet> brain,
        Activity activity,
        ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super AbstractPet>>> behaviorPriorityPairs
    ) {
        brain.addActivity(
            activity,
            behaviorPriorityPairs,
            ImmutableSet.<Pair<MemoryModuleType<?>, MemoryStatus>>of(),
            ImmutableSet.<MemoryModuleType<?>>of()
        );
    }

    private static RunOne<AbstractPet> getLookAndRandomWalk() {
        Pair<BehaviorControl<? super AbstractPet>, Integer> lookToPlayer = Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 5), 2);
        Pair<BehaviorControl<? super AbstractPet>, Integer> lookToAny = Pair.of(SetEntityLookTarget.create(MobCategory.CREATURE, 5), 2);
        Pair<BehaviorControl<? super AbstractPet>, Integer> walkRandomly = Pair.of(new RandomWalkTask(), 2);
        Pair<BehaviorControl<? super AbstractPet>, Integer> noThing = Pair.of(new DoNothing(30, 60), 1);
        return new RunOne<AbstractPet>(ImmutableList.of(lookToPlayer, lookToAny, walkRandomly, noThing));
    }
}
