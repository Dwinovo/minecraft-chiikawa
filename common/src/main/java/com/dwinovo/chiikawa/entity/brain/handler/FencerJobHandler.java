package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.dwinovo.chiikawa.entity.brain.task.fencer.MeleeAttackWithAnim;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

public final class FencerJobHandler {
    private FencerJobHandler() {
    }

    public static void initBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        BrainUtils.addCoreTasks(brain);
        BrainUtils.addIdleTasks(brain);
        addFencerTasks(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        updateActivity(brain);
    }

    private static void updateActivity(Brain<AbstractPet> brain) {
        ImmutableList.Builder<Activity> activities = ImmutableList.builder();
        // Priority: work when a target exists and no cooldown.
        if (brain.getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()
            && !brain.getMemory(MemoryModuleType.ATTACK_COOLING_DOWN).isPresent()) {
            activities.add(Activity.WORK);
        }
        activities.add(Activity.IDLE);
        // Pick the highest valid activity.
        brain.setActiveActivityToFirstValid(activities.build());
    }
    private static void addFencerTasks(Brain<AbstractPet> brain) {
        // Attack behaviors.
        Pair<Integer, BehaviorControl<? super AbstractPet>> walkToAttackTarget = Pair.of(5, SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.9F));
        Pair<Integer, BehaviorControl<? super AbstractPet>> meleeAttack = Pair.of(4, MeleeAttackWithAnim.create(20));
        BrainUtils.addActivity(brain, Activity.WORK, ImmutableList.of(walkToAttackTarget, meleeAttack));
    }
}
