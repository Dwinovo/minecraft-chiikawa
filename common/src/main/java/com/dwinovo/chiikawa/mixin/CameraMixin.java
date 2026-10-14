package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.client.photo.PhotoCapture;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a photo is being drawn the lens is the camera's, exactly: not the player's field of
 * view setting, nor what sprinting, a speed effect, being under water or another mod does to
 * it. The rest of the time this does nothing.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void chiikawa$fixedLens(float partialTick, CallbackInfoReturnable<Float> cir) {
        OptionalDouble lens = PhotoCapture.lensInUse();
        if (lens.isPresent()) {
            cir.setReturnValue((float) lens.getAsDouble());
        }
    }
}
