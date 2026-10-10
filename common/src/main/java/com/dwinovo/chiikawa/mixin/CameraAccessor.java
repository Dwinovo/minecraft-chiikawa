package com.dwinovo.chiikawa.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes how high the camera holds its eye, which it eases towards the eye height of
 * whatever it is looking through. A photo is taken from a pet's eyes in one frame, with no
 * time to ease from the player's.
 */
@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("eyeHeight")
    float chiikawa$getEyeHeight();

    @Accessor("eyeHeight")
    void chiikawa$setEyeHeight(float eyeHeight);

    @Accessor("eyeHeightOld")
    float chiikawa$getEyeHeightOld();

    @Accessor("eyeHeightOld")
    void chiikawa$setEyeHeightOld(float eyeHeight);
}
