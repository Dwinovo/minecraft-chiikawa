package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.anim.render.ShownDuring;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Hachiware's renderer adds model-specific conditional bones. The
 * {@code guitar} prop is hidden by default and only appears when some
 * controller is currently sampling the {@code guitar} animation. The
 * {@code Mouth3} expression bone, its open mouth, shows while it sings along
 * to the guitar and while it talks. The main-hand locator does the inverse of
 * the guitar so the music box item disappears during the performance instead
 * of clipping through the authored guitar prop.
 *
 * <p>Its faces: pleased, a big open-mouthed laugh with its eyes shut in arcs; hurt, no
 * tears — it grits its teeth and shouts, mouth open; brought back, moved to tears, the one
 * time it cries.
 *
 * <p>The animator originally shipped a companion {@code noguitar} animation
 * whose only keyframe is {@code guitar.scale = 0} — a hand-rolled "hide
 * guitar by default" workaround. With the {@link com.dwinovo.chiikawa.anim.render.BoneVisibilityRule}
 * pipeline that workaround is no longer needed: this rule is the source of
 * truth, the {@code noguitar} animation is dead data and can be deleted from
 * the json file.
 *
 * <p>Actually <em>playing</em> {@code guitar} (so the bone appears) is a
 * separate concern handled by gameplay code: the musician's performance sets
 * {@link com.dwinovo.chiikawa.anim.state.PetActivity#PLAY_GUITAR}, for which
 * {@link com.dwinovo.chiikawa.anim.state.PetAnimationResolver} returns
 * {@code "guitar"}. Outside a performance the guitar stays hidden — exactly
 * the "屏蔽 guitar 展示" default the model needs.
 */
public class HachiwareRenderer extends ChiikawaEntityRenderer<HachiwarePet> {
    private static final String GUITAR_ANIMATION = "guitar";

    public HachiwareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "hachiware");
        addBoneVisibilityRule("guitar",
                (state, animCtx) -> isAnyControllerPlaying(state, GUITAR_ANIMATION));
        addBoneVisibilityRule("Mouth3",
                (state, animCtx) -> isAnyControllerPlaying(state, GUITAR_ANIMATION) || isTalking(state));
        addBoneVisibilityRule("Mouth3", ShownDuring.any("happy", "hurt"));
        addBoneVisibilityRule("HappyEyes", ShownDuring.any("happy", "revive"));
        addBoneVisibilityRule("Tears", ShownDuring.any("revive"));
        addBoneVisibilityRule("RightHandLocator",
                (state, animCtx) -> !isAnyControllerPlaying(state, GUITAR_ANIMATION));
    }
}
