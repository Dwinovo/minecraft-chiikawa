package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.layer.BagLayer;
import com.dwinovo.chiikawa.entity.impl.MomongaPet;
import com.dwinovo.chiikawa.item.BagItem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Momonga's renderer. Its face is bones, built the way Chiikawa's is: it blinks with the
 * shared {@code blink} loop, and the opening under its ω ({@code Mouth3}) shows only while
 * an {@code open_mouth} animation plays. Its big tail would fill a rucksack, so under one
 * it is tucked away, as Shisa's is.
 */
public class MomongaRenderer extends ChiikawaEntityRenderer<MomongaPet> {
    public MomongaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "momonga");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("tail", (state, animCtx) -> BagLayer.wearOf(state) != BagItem.Wear.ON_BACK);
    }
}
