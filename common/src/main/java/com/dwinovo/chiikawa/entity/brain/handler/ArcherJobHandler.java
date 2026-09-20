package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.task.archer.HurtRangedAttackTargetTask;
import com.dwinovo.chiikawa.entity.brain.task.combat.CombatMoveBehavior;
import com.dwinovo.chiikawa.entity.brain.task.fencer.MeleeAttackWithAnim;
import com.dwinovo.chiikawa.init.InitActivity;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain wiring for the {@code ARCHER} pet job — ranged combat. Registered
 * statically alongside every other job's activities in
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain}; the {@code ranged}
 * intent decides when the activity runs.
 *
 * <p>Like {@link FencerJobHandler}, scopes its combat behavior to a
 * dedicated {@link InitActivity#ARCHER_SHOOT} activity rather than
 * sharing vanilla {@link Activity#WORK} (which would conflate with the
 * fencer's melee swing now that all jobs' activities live on a single
 * static brain). Leaving the activity drops the attack target.
 */
public final class ArcherJobHandler {
    private ArcherJobHandler() {
    }

    public static void registerActivities(Brain<AbstractPet> brain) {
        // Backing off is half of shooting: an archer with a zombie in its face is an
        // archer being eaten. The swing is here too, for when backing off is not an option.
        Pair<Integer, BehaviorControl<? super AbstractPet>> keepStation =
            Pair.of(5, new CombatMoveBehavior(true));
        Pair<Integer, BehaviorControl<? super AbstractPet>> shoot =
            Pair.of(4, new HurtRangedAttackTargetTask());
        Pair<Integer, BehaviorControl<? super AbstractPet>> lastResort =
            Pair.of(3, MeleeAttackWithAnim.create());
        PetActivities.register(brain, InitActivity.ARCHER_SHOOT.get(),
            ImmutableList.of(keepStation, shoot, lastResort), Set.of(MemoryModuleType.ATTACK_TARGET));
    }
}
