package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.ChiikawaPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ChiikawaRenderer extends ChiikawaEntityRenderer<ChiikawaPet> {
    public ChiikawaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "chiikawa");
    }
}
