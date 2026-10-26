package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Has creepers keep away from the pets the {@code chiikawa:creepers_avoid} tag names, the way they keep away from
 * cats: the same goal at the same priority, distance and speeds as vanilla's cat and ocelot goals in
 * {@link Creeper#registerGoals}. Only avoids; a creeper still targets players and still swells at anything it
 * is near enough to.
 */
@Mixin(Creeper.class)
public abstract class CreeperMixin extends Monster {
    private CreeperMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void chiikawa$avoidTaggedPets(CallbackInfo callback) {
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, AbstractPet.class,
            entity -> entity.getType().is(InitTag.CREEPERS_AVOID), 6.0F, 1.0, 1.2, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test));
    }
}
