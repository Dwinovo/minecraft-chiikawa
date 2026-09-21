package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.layer.BagLayer;
import com.dwinovo.chiikawa.entity.impl.ShisaPet;
import com.dwinovo.chiikawa.item.BagItem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Shisa's tail curls out further than a rucksack reaches, so under one it is tucked away
 * rather than poking through the pack. Every other pet's tail is short enough to be
 * covered by the pack itself.
 */
public class ShisaRenderer extends ChiikawaEntityRenderer<ShisaPet> {
    public ShisaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "shisa");
        addBoneVisibilityRule("tail", (state, animCtx) -> BagLayer.wearOf(state) != BagItem.Wear.ON_BACK);
    }
}
