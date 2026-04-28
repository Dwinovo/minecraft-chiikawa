package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.ShisaPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ShisaRenderer extends ChiikawaEntityRenderer<ShisaPet> {
    public ShisaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "shisa");
    }
}
