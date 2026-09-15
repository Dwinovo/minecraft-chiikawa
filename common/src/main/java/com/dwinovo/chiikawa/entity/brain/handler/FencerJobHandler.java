package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.task.fencer.MeleeAttackWithAnim;
import com.dwinovo.chiikawa.init.InitActivity;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain wiring for the {@code FENCER} pet job — melee combat. Registered
 * statically alongside every other job's activities in
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain}; brain is never
 * rebuilt on job change, and the {@code melee} intent decides when the
 * activity runs. Leaving the activity drops the attack target.
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
        PetActivities.register(brain, InitActivity.FENCER_FIGHT.get(),
            ImmutableList.of(walkToAttackTarget, meleeAttack), Set.of(MemoryModuleType.ATTACK_TARGET));
    }
}
