package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.molang.MolangContext;

/**
 * Pre-sampling stage that fills the {@link MolangContext} with values derived
 * from {@link ChiikawaRenderState} (or any other client-side source).
 *
 * <p>Conceptually the inverse of {@link BoneInterceptor}: providers run
 * <em>before</em> {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler} so
 * MoLang expressions evaluated during sampling have the right inputs;
 * interceptors run <em>after</em> sampling to override specific bone slots
 * with values that don't fit MoLang's pure-expression model.
 *
 * <p>Each provider only writes to a subset of {@link MolangContext#vars} —
 * for instance, the default {@code BasicMolangInputProvider} only sets
 * {@code query.ground_speed}. Adding a new MoLang variable means: bump the
 * slot table on {@link MolangContext}, then register a provider (or extend
 * an existing one) that fills it. The submit path itself does not change.
 *
 * <p>Providers are stateless aside from immutable configuration, just like
 * {@link com.dwinovo.chiikawa.anim.render.layer.RenderLayer}.
 */
@FunctionalInterface
public interface BoneInputProvider {

    void fill(ChiikawaRenderState state, MolangContext ctx);
}
