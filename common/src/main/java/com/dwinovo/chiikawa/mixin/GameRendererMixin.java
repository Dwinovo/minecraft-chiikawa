package com.dwinovo.chiikawa.mixin;

import com.dwinovo.chiikawa.client.photo.PhotoCapture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a photo is being drawn the game's main render target is the photo's: the level is
 * drawn into whatever the game says it draws into, and a shader pack draws into it too.
 * The rest of the time this does nothing.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "mainRenderTarget", at = @At("RETURN"), cancellable = true)
    private void chiikawa$photoTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget photo = PhotoCapture.targetInUse();
        if (photo != null) {
            cir.setReturnValue(photo);
        }
    }
}
