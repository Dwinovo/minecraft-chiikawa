package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.FuruhonyaPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Furuhonya's renderer. The model reuses Hachiware's skeleton but drops the
 * {@code guitar}/{@code string}/{@code tail} props, so the only conditional
 * bone is the {@code Mouth3} expression bone. Like Usagi, it stays hidden by
 * default — replace the {@code false} with a trigger condition (e.g.
 * {@code isAnyControllerPlaying(state, "...")}) once one exists.
 */
public class FuruhonyaRenderer extends ChiikawaEntityRenderer<FuruhonyaPet> {
    public FuruhonyaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "furuhonya");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> false);
    }
}
