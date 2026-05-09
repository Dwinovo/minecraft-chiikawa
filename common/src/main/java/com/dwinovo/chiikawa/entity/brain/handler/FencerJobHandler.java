package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.task.fencer.MeleeAttackWithAnim;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain wiring for the {@code FENCER} pet job — melee combat. Registered
 * statically alongside every other job's activities in
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain}; brain is never
 * rebuilt on job change.
 *
 * <h2>Why a dedicated FENCER_FIGHT activity</h2>
 * Earlier code reused vanilla {@link Activity#WORK} for combat behaviors,
 * which conflicted with the archer job — both jobs registering on the same
 * activity meant a fencer pet would also try to fire arrows. The dedicated
 * {@link InitActivity#FENCER_FIGHT} activity scopes melee behaviors to the
 * fencer job only, even when both jobs' activities live on the same brain.
 */
public final class FencerJobHandler {
    private FencerJobHandler() {
    }

    public static void registerActivities(Brain<AbstractPet> brain) {
        Pair<Integer, BehaviorControl<? super AbstractPet>> walkToAttackTarget =
            Pair.of(5, SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.9F));
        Pair<Integer, BehaviorControl<? super AbstractPet>> meleeAttack =
            Pair.of(4, MeleeAttackWithAnim.create(20));
        BrainUtils.addActivity(brain, InitActivity.FENCER_FIGHT.get(),
            ImmutableList.of(walkToAttackTarget, meleeAttack));
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        ImmutableList.Builder<Activity> activities = ImmutableList.builder();
        // Engage when a target exists and we aren't on attack cooldown.
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
            && !brain.hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)) {
            activities.add(InitActivity.FENCER_FIGHT.get());
        }
        activities.add(Activity.IDLE);
        brain.setActiveActivityToFirstValid(activities.build());
    }
}
