package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.UsagiPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Usagi's renderer. {@code Mouth3} is an expression bone that, like
 * Hachiware's, should stay hidden by default and only appear in a specific
 * situation. Usagi has no such situation wired up yet, so the rule simply
 * keeps it hidden — replace the {@code false} with the trigger condition
 * (e.g. {@code isAnyControllerPlaying(state, "...")}) once one exists.
 */
public class UsagiRenderer extends ChiikawaEntityRenderer<UsagiPet> {
    public UsagiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "usagi");
        addBoneVisibilityRule("Mouth3", (state, animCtx) -> false);
    }
}
