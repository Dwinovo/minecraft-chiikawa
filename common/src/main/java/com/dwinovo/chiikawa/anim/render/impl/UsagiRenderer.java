package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.UsagiPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Usagi's renderer. It is built on Chiikawa's skeleton, with its own face: it blinks with the
 * shared {@code blink} loop. Its mouth is not a ω: a stroke down the middle, the ends curling
 * up at the sides and a little tail at the bottom ({@code Mouth} and {@code Mouth2}). The
 * opening under it ({@code Mouth3}) shows only while an {@code open_mouth} animation plays.
 */
public class UsagiRenderer extends ChiikawaEntityRenderer<UsagiPet> {
    public UsagiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "usagi");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
    }
}
