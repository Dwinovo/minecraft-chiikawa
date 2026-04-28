package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HachiwareRenderer extends ChiikawaEntityRenderer<HachiwarePet> {
    public HachiwareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "hachiware");
    }
}
