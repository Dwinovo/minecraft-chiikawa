package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;

/**
 * One swing at the foe, if it can land: the pet looks at it, strikes, and holds off its next swing for as long as
 * the weapon in hand says. How long until the next one comes from the weapon rather than a fixed second, see
 * {@link PetCombat#swingCooldown}, so a sword is worth carrying for more than its damage.
 *
 * <p>An action of one tick, so it is never cut half way: an errand asked to end waits for the next tick at most, and
 * ends either before the swing or after. When the swing cannot land after all, nothing happens and the pet tries again.
 */
public final class Swing implements Step {
    private final UUID foe;

    /** @param foe the creature the fight is about */
    public Swing(UUID foe) {
        this.foe = foe;
    }

    /**
     * Whether a swing at this foe can land now: off cooldown, the foe is the pet's to fight and not a creeper that
     * would go off, the pet is not holding a bow it has arrows for, and the foe is in reach in plain sight.
     *
     * <p>Line of sight is asked of the world, not of the "nearest visible" memory: that list is refreshed every twenty
     * ticks, so a pet that had just started moving never had itself and its foe in the same snapshot, and swung at
     * nothing all the way out of the fight.
     */
    static boolean canSwing(AbstractPet pet, LivingEntity target) {
        return !pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)
            && PetTargeting.canTarget(pet, target)
            && !PetCombat.explodes(target)
            && !(isHoldingUsableProjectileWeapon(pet) && !Utils.getArrow(pet).isEmpty())
            && pet.isWithinMeleeAttackRange(target)
            && pet.hasLineOfSight(target);
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet pet = live.pet();
        Skirmish.hunted(live, foe).filter(target -> canSwing(pet, target)).ifPresent(target -> {
            handle.lookAt(target);
            handle.action(PetAction.SLASH);
            handle.swing();
            pet.doHurtTarget(target);
            pet.playAttackSound();
            handle.cooldownAttack(PetCombat.swingCooldown(attackSpeedOf(pet)));
        });
        return StepStatus.DONE;
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
