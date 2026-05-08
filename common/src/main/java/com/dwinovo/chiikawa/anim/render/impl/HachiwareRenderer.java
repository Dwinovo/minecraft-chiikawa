package com.dwinovo.chiikawa.anim.render.impl;

import com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Hachiware's renderer adds one model-specific concern: the {@code guitar}
 * bone (a child of {@code Root}, 22 cubes) is hidden by default and only
 * appears when some controller is currently sampling the {@code guitar}
 * animation — a static "holding guitar" pose authored by the modeler.
 *
 * <p>The animator originally shipped a companion {@code noguitar} animation
 * whose only keyframe is {@code guitar.scale = 0} — a hand-rolled "hide
 * guitar by default" workaround. With the {@link com.dwinovo.chiikawa.anim.render.BoneVisibilityRule}
 * pipeline that workaround is no longer needed: this rule is the source of
 * truth, the {@code noguitar} animation is dead data and can be deleted from
 * the json file.
 *
 * <p>Wiring a controller to actually <em>play</em> {@code guitar} (so the
 * bone appears) is a separate concern handled by gameplay code — e.g.
 * extending {@code PetMode} with a {@code PLAYING_GUITAR} value and having
 * {@link com.dwinovo.chiikawa.anim.state.PetAnimationResolver} return
 * {@code "guitar"} in that mode. Until then, hachiware never enters the
 * mode and the guitar stays hidden — exactly the "屏蔽 guitar 展示" default
 * the model needs.
 */
public class HachiwareRenderer extends ChiikawaEntityRenderer<HachiwarePet> {
    public HachiwareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "hachiware");
        addBoneVisibilityRule("guitar",
                (state, animCtx) -> isAnyControllerPlaying(state, "guitar"));
    }
}
