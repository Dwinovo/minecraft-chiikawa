package com.dwinovo.chiikawa.entity.brain.task.fencer;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;

public final class MeleeAttackWithAnim {
    private MeleeAttackWithAnim() {
    }

    public static OneShot<AbstractPet> create(int cooldownBetweenAttacks) {
        return BehaviorBuilder.create(
            brain -> brain.group(
                    brain.registered(MemoryModuleType.LOOK_TARGET),
                    brain.present(MemoryModuleType.ATTACK_TARGET),
                    brain.absent(MemoryModuleType.ATTACK_COOLING_DOWN),
                    brain.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                )
                .apply(
                    brain,
                    (lookTarget, attackTarget, cooldown, visibleTargets) -> (ServerLevel level, AbstractPet pet, long time) -> {
                        LivingEntity target = brain.get(attackTarget);
                        if (isHoldingUsableProjectileWeapon(pet)
                            || !pet.isWithinMeleeAttackRange(target)
                            || !brain.<NearestVisibleLivingEntities>get(visibleTargets).contains(target)) {
                            return false;
                        }
                        lookTarget.set(new EntityTracker(target, true));
                        pet.triggerAnim("use_mainhand");
                        pet.swing(InteractionHand.MAIN_HAND);
                        pet.doHurtTarget(level, target);
                        pet.playAttackSound();
                        cooldown.setWithExpiry(true, (long) cooldownBetweenAttacks);
                        return true;
                    }
                )
        );
    }

    private static boolean isHoldingUsableProjectileWeapon(AbstractPet pet) {
        return pet.isHolding(stack -> {
            Item item = stack.getItem();
            return item instanceof ProjectileWeaponItem && pet.canUseNonMeleeWeapon(stack);
        });
    }
}

