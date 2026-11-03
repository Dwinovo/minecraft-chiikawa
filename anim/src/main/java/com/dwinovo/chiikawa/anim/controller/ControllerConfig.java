package com.dwinovo.chiikawa.anim.controller;

/**
 * Static configuration for a single animation controller. Registered once per
 * {@link ControllerRoster} (shared across every entity that roster animates),
 * then materialised into one mutable {@link ControllerInstance} per entity.
 *
 * <p>Controllers are evaluated in registration order — within a frame, every
 * controller writes into the same shared pose buffer, so a controller
 * registered later sees (and may override / add onto) values written by
 * earlier controllers. This is the GeckoLib model: the order is the priority.
 *
 * <h2>Naming</h2>
 * The {@code name} is used as the lookup key for one-shot triggers
 * ({@link com.dwinovo.chiikawa.anim.runtime.Animator#playOnce}, e.g. a gameplay event dispatching an action to
 * the {@code "action"} controller). Two controllers on the same roster must
 * not share a name.
 *
 * @param name          stable identifier used to address this controller for
 *                      external one-shot triggers
 * @param blendMode     how this controller's sampled values combine with values
 *                      already in the pose buffer
 * @param transitionSec crossfade length in seconds when the handler swaps
 *                      animations; {@code 0} disables crossfading on this
 *                      controller (typical for constant decorative loops)
 * @param handler       state-driven animation picker; must be a pure function
 *                      of its input to keep advancing idempotent. May be
 *                      {@link ControllerHandler#neverPlay}
 *                      for controllers that only run on external triggers
 *                      ({@code playOnce}).
 * @param <I>           what the handler picks by, see {@link ControllerHandler}
 */
public record ControllerConfig<I>(
        String name,
        BlendMode blendMode,
        float transitionSec,
        ControllerHandler<I> handler
) {
    public ControllerConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("controller name must be non-blank");
        }
        if (blendMode == null) blendMode = BlendMode.OVERRIDE;
        if (transitionSec < 0f) transitionSec = 0f;
        if (handler == null) handler = ControllerHandler::neverPlay;
    }
}
