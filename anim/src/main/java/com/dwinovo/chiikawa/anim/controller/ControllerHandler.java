package com.dwinovo.chiikawa.anim.controller;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;

/**
 * Pure function from the caller's per-frame state to "what should this controller play
 * right now". Called every time the animator advances, must be idempotent —
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
 *
 * @param <I> what the caller hands the animator each frame for its handlers to pick by;
 *            the engine never looks inside it
 */
@FunctionalInterface
public interface ControllerHandler<I> {
    /**
     * @param input the caller's state for this frame
     * @return animation to play, or {@code null} for "no contribution"
     */
    BakedAnimation handle(I input);

    /**
     * Handler for controllers that only run on external triggers — the state
     * machine never picks an animation for them. For controllers that receive
     * their animations only via {@link com.dwinovo.chiikawa.anim.runtime.Animator#playOnce}.
     */
    static <I> BakedAnimation neverPlay(I input) {
        return null;
    }
}
