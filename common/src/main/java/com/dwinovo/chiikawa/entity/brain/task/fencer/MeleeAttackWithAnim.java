package com.dwinovo.chiikawa.entity.brain.task.fencer;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat;
import com.dwinovo.chiikawa.utils.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;

public final class MeleeAttackWithAnim {
    private MeleeAttackWithAnim() {
    }

    /**
     * The swing itself, on its own so both jobs can run it: a fencer's whole fight, and an
     * archer's answer to something that got inside its bow.
     *
     * <p>How long until the next one comes from the weapon in hand rather than a fixed
     * second — {@link PetCombat#swingCooldown} — so a sword is worth carrying for more
     * than its damage.
     */
    public static OneShot<AbstractPet> create() {
        return BehaviorBuilder.create(
            brain -> brain.group(
                    brain.registered(MemoryModuleType.LOOK_TARGET),
                    brain.present(MemoryModuleType.ATTACK_TARGET),
                    brain.absent(MemoryModuleType.ATTACK_COOLING_DOWN)
                )
                .apply(
                    brain,
                    (lookTarget, attackTarget, cooldown) -> (ServerLevel level, AbstractPet pet, long time) -> {
                        LivingEntity target = brain.get(attackTarget);
                        // Line of sight is asked of the world, not of the "nearest visible"
                        // memory: that list is refreshed every twenty ticks, so a pet that
                        // had just started moving never had itself and its target in the
                        // same snapshot, and swung at nothing all the way out of the fight.
                        if (!PetTargeting.canTarget(pet, target)
                            || PetCombat.explodes(target)
                            || (isHoldingUsableProjectileWeapon(pet) && !Utils.getArrow(pet).isEmpty())
                            || !pet.isWithinMeleeAttackRange(target)
                            || !pet.hasLineOfSight(target)) {
                            return false;
                        }
                        lookTarget.set(new EntityTracker(target, true));
                        pet.triggerAction(PetAction.SLASH);
                        pet.swing(InteractionHand.MAIN_HAND);
                        pet.doHurtTarget(level, target);
                        pet.playAttackSound();
                        cooldown.setWithExpiry(true, (long) PetCombat.swingCooldown(attackSpeedOf(pet)));
                        return true;
                    }
                )
        );
    }

    /** What the game says about the weapon in hand; 0 when the pet has no such attribute. */
    private static double attackSpeedOf(AbstractPet pet) {
        AttributeInstance speed = pet.getAttribute(Attributes.ATTACK_SPEED);
        return speed == null ? 0.0 : speed.getValue();
    }

    private static boolean isHoldingUsableProjectileWeapon(AbstractPet pet) {
        return pet.isHolding(stack -> {
            Item item = stack.getItem();
            return item instanceof ProjectileWeaponItem weapon && pet.canFireProjectileWeapon(weapon);
        });
    }
}
