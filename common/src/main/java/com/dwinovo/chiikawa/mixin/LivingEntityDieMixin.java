package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.task.TaskTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts a monster towards the hunting slip of the pet that put it down.
 *
 * <p>The rule is <b>who dealt the fatal blow</b>, not who the game hands the kill to.
 * Vanilla's kill credit goes to the last player who hurt the thing, so an owner fighting
 * beside their own pet quietly took every kill off its slip: a scratch from the owner and
 * then the pet's killing blow counted for nothing, which is exactly what a fight beside a
 * pet looks like. The blow is read from the damage source, so an arrow counts for the pet
 * that shot it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDieMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void chiikawa$countTowardsHuntingSlip(DamageSource source, CallbackInfo callback) {
        LivingEntity dying = (LivingEntity) (Object) this;
        if (dying.level().isClientSide() || !(dying instanceof Enemy)) {
            return;
        }
        if (source.getEntity() instanceof AbstractPet pet && pet != dying) {
            TaskTracker.advance(pet, PetWorkCounters.SLAY, 1);
        }
    }
}
