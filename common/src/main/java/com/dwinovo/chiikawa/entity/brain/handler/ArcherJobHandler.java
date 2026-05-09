package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.entity.brain.task.archer.HurtRangedAttackTargetTask;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.dwinovo.chiikawa.utils.Utils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain wiring for the {@code ARCHER} pet job — ranged combat. Registered
 * statically alongside every other job's activities in
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain}.
 *
 * <p>Like {@link FencerJobHandler}, scopes its combat behavior to a
 * dedicated {@link InitActivity#ARCHER_SHOOT} activity rather than
 * sharing vanilla {@link Activity#WORK} (which would conflate with the
 * fencer's melee swing now that all jobs' activities live on a single
 * static brain).
 */
public final class ArcherJobHandler {
    private ArcherJobHandler() {
    }

    public static void registerActivities(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> hurtRangedAttackTarget =
            Pair.of(4, new HurtRangedAttackTargetTask());
        BrainUtils.addActivity(brain, InitActivity.ARCHER_SHOOT.get(),
            ImmutableList.of(hurtRangedAttackTarget));
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        // Outside WORK mode the archer shouldn't engage — clear stale combat
        // memories from prior engagements so the bow doesn't auto-fire while
        // the player is just walking around with the pet.
        if (pet.getPetMode() != PetMode.WORK) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.ATTACK_COOLING_DOWN);
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
            return;
        }
        ImmutableList.Builder<Activity> activities = ImmutableList.builder();
        // Engage when a target exists, ammo is available, and we aren't on cooldown.
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
            && !Utils.getArrow(pet).isEmpty()
            && !brain.hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)) {
            activities.add(InitActivity.ARCHER_SHOOT.get());
        }
        activities.add(Activity.IDLE);
        brain.setActiveActivityToFirstValid(activities.build());
    }
}
