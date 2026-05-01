package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;

/**
 * Per-entity animator. Owns the small mutable state needed to drive the
 * pose sampler — a stack of channels (main animation + transient triggered
 * animations) and nothing else.
 *
 * <p>Layer 0 owns the current base loop. Upper layers are transient slots for
 * semantic action, overlay, and reaction events.
 *
 * <p>State changes go through {@link #setMain(BakedAnimation, boolean)} or
 * {@link #trigger(int, BakedAnimation)} — both replace the channel record
 * wholesale rather than mutating, preserving the pure-function sampling
 * contract.
 */
public final class PetAnimator {

    public static final float DEFAULT_BASE_TRANSITION_SEC = 0.16f;

    public static final int LAYER_BASE = 0;
    public static final int LAYER_ACTION = 1;
    public static final int LAYER_OVERLAY = 2;
    public static final int LAYER_REACTION = 3;

    /** Number of layered channels. */
    public static final int CHANNEL_COUNT = 4;

    private final AnimationChannel[] channels = new AnimationChannel[CHANNEL_COUNT];

    /** Returns the channel at {@code layer}, or {@code null} if nothing is playing there. */
    public AnimationChannel get(int layer) {
        return channels[layer];
    }

    /** Starts a looping main animation if not already playing this exact one. */
    public void setMain(BakedAnimation animation, boolean looping) {
        setMain(animation, looping, DEFAULT_BASE_TRANSITION_SEC);
    }

    /** Starts a main animation, crossfading from the previous main channel when it changes. */
    public void setMain(BakedAnimation animation, boolean looping, float transitionSec) {
        AnimationChannel current = channels[LAYER_BASE];
        if (current != null && current.animation() == animation && current.looping() == looping) {
            return;
        }
        long nowNs = System.nanoTime();
        AnimationTransition transition = null;
        if (current != null && transitionSec > 0f) {
            transition = new AnimationTransition(current.withoutTransition(), nowNs, transitionSec);
        }
        channels[LAYER_BASE] = new AnimationChannel(animation, nowNs, looping, transition);
    }

    /** Triggers a non-looping animation on {@code layer}. Idempotent at the network level. */
    public void trigger(int layer, BakedAnimation animation) {
        channels[layer] = new AnimationChannel(animation, System.nanoTime(), false);
    }

    /** Clears finished transitions and non-looping upper channels. */
    public void clearFinished(long nowNs) {
        AnimationChannel base = channels[LAYER_BASE];
        if (base != null && isTransitionFinished(base.transition(), nowNs)) {
            channels[LAYER_BASE] = base.withoutTransition();
        }
        for (int layer = 1; layer < CHANNEL_COUNT; layer++) {
            if (isFinished(channels[layer], nowNs)) {
                channels[layer] = null;
            }
        }
    }

    /** Returns whether this one-shot channel has reached the end of its baked duration. */
    public static boolean isFinished(AnimationChannel channel, long nowNs) {
        if (channel == null || channel.looping()) {
            return false;
        }
        BakedAnimation animation = channel.animation();
        if (animation == null || animation.durationSec <= 0f) {
            return true;
        }
        long elapsedNs = nowNs - channel.startTimeNs();
        if (elapsedNs < 0L) {
            return false;
        }
        return elapsedNs >= (long) (animation.durationSec * 1_000_000_000L);
    }

    public static boolean isTransitionFinished(AnimationTransition transition, long nowNs) {
        if (transition == null) {
            return false;
        }
        if (transition.durationSec() <= 0f) {
            return true;
        }
        long elapsedNs = nowNs - transition.startTimeNs();
        return elapsedNs >= (long) (transition.durationSec() * 1_000_000_000L);
    }

    /** Clears the channel — used when a one-shot finishes. */
    public void clear(int layer) {
        channels[layer] = null;
    }
}
