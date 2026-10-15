package com.dwinovo.chiikawa.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Sets whose eyes the game looks through, which is all the level is drawn from, without the
 * game's own setter: that one also resets the screen effect the player has on. A photo is
 * taken from a pet's eyes and the player's effect is to stay as it is.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("cameraEntity")
    void chiikawa$setCameraEntity(@Nullable Entity cameraEntity);
}
