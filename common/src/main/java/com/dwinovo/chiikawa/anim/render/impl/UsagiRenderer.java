package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.ShownDuring;
import com.dwinovo.chiikawa.entity.impl.UsagiPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Usagi's renderer. It is built on Chiikawa's skeleton, with its own face: it blinks with the
 * shared {@code blink} loop. Its mouth is not a ω: a stroke down the middle, the ends curling
 * up at the sides and a little tail at the bottom ({@code Mouth} and {@code Mouth2}). The
 * opening under it ({@code Mouth3}) shows only while an {@code open_mouth} animation plays,
 * and while it is pleased, when it jumps with its mouth wide open. It never cries: hurt, it
 * glares and stamps, which its animation does with no parts of its own.
 */
public class UsagiRenderer extends ChiikawaEntityRenderer<UsagiPet> {
    public UsagiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "usagi");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("Mouth3", ShownDuring.any("happy"));
    }
}
