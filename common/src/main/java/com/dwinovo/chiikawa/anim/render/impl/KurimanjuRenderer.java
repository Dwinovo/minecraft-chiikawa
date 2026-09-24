package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.ShownDuring;
import com.dwinovo.chiikawa.entity.impl.KurimanjuPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Kurimanju's renderer. Its face is bones, built the way Chiikawa's is: it blinks with the
 * shared {@code blink} loop, and the opening under its ω ({@code Mouth3}) shows only while
 * an {@code open_mouth} animation plays.
 *
 * <p>Its faces are a grown-up's: pleased, eyes shut in a contented sigh; hurt, it bears it
 * without a sound; puzzled, it says nothing, and three dots hang over its head.
 */
public class KurimanjuRenderer extends ChiikawaEntityRenderer<KurimanjuPet> {
    public KurimanjuRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "kurimanju");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("HappyEyes", ShownDuring.any("happy"));
        addBoneVisibilityRule("Dots", ShownDuring.any("confused"));
    }
}
