package com.dwinovo.chiikawa.anim.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Render-state snapshot for chiikawa-animated entities. Extends vanilla
 * {@link LivingEntityRenderState} (so all standard fields are populated by
 * the vanilla extract pipeline) and adds the model / texture identity that
 * the renderer uses to look up baked data and bind a texture.
 *
 * <p>This class deliberately does <em>not</em> implement
 * {@code GeoRenderState} or carry any GeckoLib data ticket map — animation
 * timing is read from the entity's {@code PetAnimator}, not from the state.
 */
public class ChiikawaRenderState extends LivingEntityRenderState {
    /** Identifier under which the model was registered in {@link com.dwinovo.chiikawa.anim.api.ModelLibrary}. */
    public Identifier modelKey;
    /** Texture path. */
    public Identifier texture;
}
