package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.task.archer.HurtRangedAttackTargetTask;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

public final class ArcherJobHandler {
    private ArcherJobHandler() {
    }

    public static void initBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        BrainUtils.addCoreTasks(brain);
        BrainUtils.addIdleTasks(brain);

        addFencerTasks(brain);
        // Core activities.
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        // Default activity.
        brain.setDefaultActivity(Activity.IDLE);
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        if (pet.getPetMode() != com.dwinovo.chiikawa.entity.PetMode.WORK) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.ATTACK_COOLING_DOWN);
            
            return;
        }
        ImmutableList.Builder<Activity> activities = ImmutableList.builder();
        // WORK requires a target, ammo, and no cooldown.
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
        && !Utils.getArrow(pet).isEmpty()
        && !brain.hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN))
        {
            activities.add(Activity.WORK);
        }
        // Always allow idle.
        activities.add(Activity.IDLE);
        // Pick the highest valid activity.
        brain.setActiveActivityToFirstValid(activities.build());
        
    }
    public static void addFencerTasks(Brain<AbstractPet> brain){
        Pair<Integer, BehaviorControl<? super AbstractPet>> hurtRangedAttackTarget = Pair.of(4, new HurtRangedAttackTargetTask());
        BrainUtils.addActivity(brain, Activity.WORK, ImmutableList.of(hurtRangedAttackTarget));

    }
}
