package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;

/**
 * Programmatic post-processing pass that runs <em>after</em>
 * {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler} and <em>before</em>
 * {@link ModelRenderer}, mutating the pose buffer in place.
 *
 * <p>This hook is where the renderer overrides specific bones with values
 * derived from live entity state (head look-at, ear sway, tail wag) rather than from the animation
 * file. Working on the flat {@code float[]} pose buffer instead of the
 * {@code PoseStack} avoids redundant matrix push/pop pairs and keeps the
 * data layout JNI-friendly for a future Rust port.
 *
 * <p>Interceptors completely override the affected bone slots — they do
 * not blend with the animation's contribution to those slots. This matches
 * the legacy {@code BoneSnapshots#ifPresent(name, snap -> snap.setRot…)}
 * semantics in {@code AbstractPetRender}.
 */
@FunctionalInterface
public interface BoneInterceptor {

    void apply(BakedModel model, ChiikawaRenderState state, MolangContext ctx, float[] poseBuf);
}
