package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.client.photo.PhotoCapture;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a photo is being drawn the lens is the camera's, exactly: not the player's field of
 * view setting, nor what sprinting, a speed effect, being under water or another mod does to
 * it. The rest of the time this does nothing.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void chiikawa$fixedLens(Camera camera, float partialTick, boolean useFovSetting,
            CallbackInfoReturnable<Float> cir) {
        OptionalDouble lens = PhotoCapture.lensInUse();
        if (lens.isPresent()) {
            cir.setReturnValue((float) lens.getAsDouble());
        }
    }
}
