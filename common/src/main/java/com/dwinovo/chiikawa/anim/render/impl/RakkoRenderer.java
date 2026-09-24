package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.layer.BagLayer;
import com.dwinovo.chiikawa.entity.impl.RakkoPet;
import com.dwinovo.chiikawa.item.BagItem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Rakko's renderer. Its face is bones: it blinks with the shared {@code blink} loop, and the
 * opening under its mouth ({@code Mouth3}) shows only while an {@code open_mouth} animation
 * plays. Its cape would go through a rucksack, so under one it is tucked away, as Shisa's
 * and Momonga's tails are.
 */
public class RakkoRenderer extends ChiikawaEntityRenderer<RakkoPet> {
    public RakkoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "rakko");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("Cape", (state, animCtx) -> BagLayer.wearOf(state) != BagItem.Wear.ON_BACK);
    }
}
