package com.dwinovo.chiikawa.gametest.mixin;

import java.util.List;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears a case's floor the moment it passes, as {@code GameTestInfo.succeed} does from
 * 1.20.4 on. 1.20.2 leaves everything on it running until the batch is over, so a pet
 * whose case has passed goes on working, and walks into the cases still going beside it:
 * a fencer finds another case's zombie, a farmer another case's slip.
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin {
    @Shadow
    public abstract Throwable getError();

    @Shadow
    public abstract AABB getStructureBounds();

    @Shadow
    public abstract ServerLevel getLevel();

    @Inject(method = "succeed", at = @At("TAIL"))
    private void chiikawa$clearPassedCase(CallbackInfo callback) {
        if (getError() == null) {
            List<Entity> left = getLevel().getEntitiesOfClass(Entity.class, getStructureBounds().inflate(1.0),
                entity -> !(entity instanceof Player));
            left.forEach(entity -> entity.remove(Entity.RemovalReason.DISCARDED));
        }
    }
}
