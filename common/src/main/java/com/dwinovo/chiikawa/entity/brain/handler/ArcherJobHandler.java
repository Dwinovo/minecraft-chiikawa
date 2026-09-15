package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.task.archer.HurtRangedAttackTargetTask;
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
        Pair<Integer, BehaviorControl<? super AbstractPet>> hurtRangedAttackTarget =
            Pair.of(4, new HurtRangedAttackTargetTask());
        PetActivities.register(brain, InitActivity.ARCHER_SHOOT.get(),
            ImmutableList.of(hurtRangedAttackTarget), Set.of(MemoryModuleType.ATTACK_TARGET));
    }
}
