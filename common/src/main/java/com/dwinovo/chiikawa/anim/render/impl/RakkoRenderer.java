package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.RakkoPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RakkoRenderer extends ChiikawaEntityRenderer<RakkoPet> {
    public RakkoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "rakko");
    }
}
