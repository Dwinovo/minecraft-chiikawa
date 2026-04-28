package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;

/**
 * Per-entity animator. Owns the small mutable state needed to drive the
 * pose sampler — a stack of channels (main animation + transient triggered
 * animations) and nothing else.
 *
 * <p>Phase 2 only uses channel 0 (main idle/walk loop). Phase 3 will add
 * upper channels for triggered one-shots and additive blends.
 *
 * <p>State changes go through {@link #setMain(BakedAnimation, boolean)} or
 * {@link #trigger(int, BakedAnimation)} — both replace the channel record
 * wholesale rather than mutating, preserving the pure-function sampling
 * contract.
 */
public final class PetAnimator {

    /** Number of layered channels. Phase 2 uses only index 0. */
    public static final int CHANNEL_COUNT = 4;

    private final AnimationChannel[] channels = new AnimationChannel[CHANNEL_COUNT];

    /** Returns the channel at {@code layer}, or {@code null} if nothing is playing there. */
    public AnimationChannel get(int layer) {
        return channels[layer];
    }

    /** Starts a looping main animation if not already playing this exact one. */
    public void setMain(BakedAnimation animation, boolean looping) {
        AnimationChannel current = channels[0];
        if (current != null && current.animation() == animation && current.looping() == looping) {
            return;
        }
        channels[0] = new AnimationChannel(animation, System.nanoTime(), looping);
    }

    /** Triggers a non-looping animation on {@code layer}. Idempotent at the network level. */
    public void trigger(int layer, BakedAnimation animation) {
        channels[layer] = new AnimationChannel(animation, System.nanoTime(), false);
    }

    /** Clears the channel — used when a one-shot finishes. */
    public void clear(int layer) {
        channels[layer] = null;
    }
}
