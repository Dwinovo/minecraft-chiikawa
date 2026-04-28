package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.KurimanjuPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class KurimanjuRenderer extends ChiikawaEntityRenderer<KurimanjuPet> {
    public KurimanjuRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "kurimanju");
    }
}
