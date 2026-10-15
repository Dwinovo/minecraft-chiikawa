package com.dwinovo.chiikawa.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the shaders' global settings, where the game writes the position of the camera the
 * terrain is placed by. A photo is drawn from a pet's eyes inside the player's frame and has to
 * write it for the pet's camera itself.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("globalSettingsUniform")
    GlobalSettingsUniform chiikawa$getGlobalSettingsUniform();
}
