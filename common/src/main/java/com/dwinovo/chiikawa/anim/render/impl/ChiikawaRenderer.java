package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.entity.impl.ChiikawaPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Chiikawa's renderer. Its face is bones, as Usagi's and Hachiware's are, so it blinks with
 * the shared {@code blink} loop; its mouth is two of them, shut ({@code Mouth2}) and open
 * ({@code Mouth3}), and only one shows at a time: open while an {@code open_mouth} animation
 * plays, shut the rest of the time.
 */
public class ChiikawaRenderer extends ChiikawaEntityRenderer<ChiikawaPet> {
    public ChiikawaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "chiikawa");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> talking(state));
        addBoneVisibilityRule("Mouth2", (state, animCtx) -> !talking(state));
    }

    private static boolean talking(ChiikawaRenderState state) {
        return isAnyControllerPlaying(state, "open_mouth1") || isAnyControllerPlaying(state, "open_mouth2");
    }
}
