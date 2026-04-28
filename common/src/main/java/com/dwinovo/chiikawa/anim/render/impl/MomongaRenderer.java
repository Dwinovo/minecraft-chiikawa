package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.MomongaPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MomongaRenderer extends ChiikawaEntityRenderer<MomongaPet> {
    public MomongaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "momonga");
    }
}
