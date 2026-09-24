package com.dwinovo.chiikawa.fabric.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the mod name its own special item renderer, the one a prop's item model asks for.
 * NeoForge has an event for this; Fabric has no registry of its own, so the mod adds to
 * vanilla's, which vanilla keeps private.
 */
@Mixin(SpecialModelRenderers.class)
public interface SpecialModelRenderersAccessor {
    @Accessor("ID_MAPPER")
    static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked>> chiikawa$getIdMapper() {
        throw new AssertionError();
    }
}
