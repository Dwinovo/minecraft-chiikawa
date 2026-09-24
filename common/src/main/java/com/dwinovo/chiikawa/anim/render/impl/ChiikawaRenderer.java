package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.ShownDuring;
import com.dwinovo.chiikawa.entity.impl.ChiikawaPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Chiikawa's renderer. Its face is bones, as Usagi's and Hachiware's are, so it blinks with
 * the shared {@code blink} loop. Its mouth opens as Usagi's does: the ω ({@code Mouth})
 * stays as the upper lip, and the opening under its middle ({@code Mouth3}) grows down out
 * of it — there only while an {@code open_mouth} animation plays.
 *
 * <p>Its faces are the ones the series gives it: pleased, its eyes shut in two arcs; hurt,
 * it cries, eyes screwed up and tears running, as it does whenever something goes wrong;
 * puzzled, a drop of sweat; brought back, laughing and crying at once.
 */
public class ChiikawaRenderer extends ChiikawaEntityRenderer<ChiikawaPet> {
    public ChiikawaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "chiikawa");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("HappyEyes", ShownDuring.any("happy", "revive"));
        addBoneVisibilityRule("CryEyes", ShownDuring.any("hurt"));
        addBoneVisibilityRule("Tears", ShownDuring.any("hurt", "revive"));
        addBoneVisibilityRule("Sweat", ShownDuring.any("confused"));
    }
}
