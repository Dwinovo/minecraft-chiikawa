package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.ShownDuring;
import com.dwinovo.chiikawa.anim.render.layer.BagLayer;
import com.dwinovo.chiikawa.entity.impl.MomongaPet;
import com.dwinovo.chiikawa.item.BagItem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Momonga's renderer. Its face is bones, built the way Chiikawa's is: it blinks with the
 * shared {@code blink} loop, and the opening under its ω ({@code Mouth3}) shows only while
 * an {@code open_mouth} animation plays. Its big tail would fill a rucksack, so under one
 * it is tucked away, as Shisa's is.
 *
 * <p>Its faces: pleased with itself, eyes shut in arcs, and the same arcs asleep or when it
 * plays cute; hurt, a loud cry, eyes screwed up and tears running — not always a real one;
 * yawning, eyes screwed up; brought back, all sweetness.
 */
public class MomongaRenderer extends ChiikawaEntityRenderer<MomongaPet> {
    public MomongaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "momonga");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("tail", (state, animCtx) -> BagLayer.wearOf(state) != BagItem.Wear.ON_BACK);
        addBoneVisibilityRule("HappyEyes", ShownDuring.any("happy", "sleep", "cute", "revive"));
        addBoneVisibilityRule("CryEyes", ShownDuring.any("hurt", "yawn"));
        addBoneVisibilityRule("Tears", ShownDuring.any("hurt"));
    }
}
