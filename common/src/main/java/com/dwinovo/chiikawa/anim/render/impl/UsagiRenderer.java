package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.UsagiPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class UsagiRenderer extends ChiikawaEntityRenderer<UsagiPet> {
    public UsagiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "usagi");
    }
}
