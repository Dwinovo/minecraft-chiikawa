package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.layer.BagLayer;
import com.dwinovo.chiikawa.entity.impl.ShisaPet;
import com.dwinovo.chiikawa.item.BagItem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Shisa's renderer. Its face is bones: it blinks with the shared {@code blink} loop, and the
 * opening under its mouth ({@code Mouth3}) shows only while an {@code open_mouth} animation
 * plays. Its tail curls out further than a rucksack reaches, so under one it is tucked away
 * rather than poking through the pack, as Momonga's is. Every other pet's tail is short
 * enough to be covered by the pack itself.
 */
public class ShisaRenderer extends ChiikawaEntityRenderer<ShisaPet> {
    public ShisaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "shisa");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> isTalking(state));
        addBoneVisibilityRule("tail", (state, animCtx) -> BagLayer.wearOf(state) != BagItem.Wear.ON_BACK);
    }
}
