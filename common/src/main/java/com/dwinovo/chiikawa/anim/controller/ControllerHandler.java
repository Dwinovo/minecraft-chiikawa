package com.dwinovo.chiikawa.anim.controller;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import org.jspecify.annotations.Nullable;

/**
 * Pure function from gameplay state to "what should this controller play
 * right now". Called every render-state extract, must be idempotent —
 * returning the same animation in two consecutive frames is a no-op (the
 * controller short-circuits on equality).
 *
 * <p>Returning {@code null} signals "this controller contributes nothing this
 * frame". The pose buffer is left untouched at this controller's stage —
 * later controllers and interceptors continue normally.
 *
 * <p>This is the GeckoLib {@code AnimationStateHandler} pattern, narrowed to
 * the data this codebase actually has: there's no {@code RawAnimation} stage
 * chain, no {@code PlayState} tri-state — just "play this animation, or
 * nothing".
 */
@FunctionalInterface
public interface ControllerHandler {
    /**
     * @param state per-frame render-state snapshot
     * @param ctx   gameplay state snapshot (mode / locomotion / job ...)
     * @return animation to play, or {@code null} for "no contribution"
     */
    @Nullable BakedAnimation handle(ChiikawaRenderState state, PetAnimContext ctx);

    /**
     * Handler for controllers that only run on external triggers — the state
     * machine never picks an animation for them. Used by the {@code "action"}
     * and {@code "reaction"} controllers, which receive their animations via
     * {@link com.dwinovo.chiikawa.anim.runtime.PetAnimator#playOnce}.
     */
    static @Nullable BakedAnimation neverPlay(ChiikawaRenderState state, PetAnimContext ctx) {
        return null;
    }
}
