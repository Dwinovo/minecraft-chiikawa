package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.FuruhonyaPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Furuhonya's renderer. It is built on Chiikawa's skeleton, with its own face: it blinks with
 * the shared {@code blink} loop, and its mouth is a closed ω, as the plush has it. The opening
 * under the ω ({@code Mouth3}) and the line inside it ({@code Mouth2}) show only while an
 * {@code open_mouth} animation plays.
 */
public class FuruhonyaRenderer extends ChiikawaEntityRenderer<FuruhonyaPet> {
    public FuruhonyaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "furuhonya");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("Mouth2", (state, animCtx) -> isTalking(state));
    }
}
