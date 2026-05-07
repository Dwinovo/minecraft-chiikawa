package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;

/**
 * Per-entity animator. Owns the small mutable state needed to drive the
 * pose sampler — a slot table of channels (main animation + transient
 * triggered animations) and nothing else.
 *
 * <p>Slots are an {@link Slot} enum: {@link Slot#BASE} owns the current
 * looping animation; the upper slots are transient pockets for semantic
 * action, overlay, and reaction events. Adding a new slot is backward-
 * compatible — append to the enum, no array sizing constants to keep in sync.
 *
 * <p>State changes go through {@link #setMain(BakedAnimation, boolean)} or
 * {@link #trigger(Slot, BakedAnimation)} — both replace the channel record
 * wholesale rather than mutating, preserving the pure-function sampling
 * contract.
 */
public final class PetAnimator {

    public static final float DEFAULT_BASE_TRANSITION_SEC = 0.16f;

    /**
     * Stable slot identity.
     *
     * <p>Each slot owns at most one {@link AnimationChannel}. {@link #BASE} is
     * the looping foundation; non-base slots are transient one-shots reaped
     * by {@link #clearFinished(long)}.
     */
    public enum Slot {
        /** Looping foundation animation (idle / walk / run / sit ...). */
        BASE,
        /** Semantic gameplay action (use item, harvest, attack ...). */
        ACTION,
        /** Reserved upper-body overlay (held-prop emotes, future use). */
        OVERLAY,
        /** Short-lived emotional reaction (hurt, happy, scratch_head ...). */
        REACTION;

        /** Cached values() to avoid per-frame allocation; treat as immutable. */
        public static final Slot[] VALUES = values();
    }

    private final AnimationChannel[] channels = new AnimationChannel[Slot.VALUES.length];

    /** Returns the channel at {@code slot}, or {@code null} if nothing is playing there. */
    public AnimationChannel get(Slot slot) {
        return channels[slot.ordinal()];
    }

    /** Starts a looping main animation if not already playing this exact one. */
    public void setMain(BakedAnimation animation, boolean looping) {
        setMain(animation, looping, DEFAULT_BASE_TRANSITION_SEC);
    }

    /** Starts a main animation, crossfading from the previous main channel when it changes. */
    public void setMain(BakedAnimation animation, boolean looping, float transitionSec) {
        AnimationChannel current = channels[Slot.BASE.ordinal()];
        if (current != null && current.animation() == animation && current.looping() == looping) {
            return;
        }
        long nowNs = System.nanoTime();
        AnimationTransition transition = null;
        if (current != null && transitionSec > 0f) {
            transition = new AnimationTransition(current.withoutTransition(), nowNs, transitionSec);
        }
        channels[Slot.BASE.ordinal()] = new AnimationChannel(animation, nowNs, looping, transition);
    }

    /** Triggers a non-looping animation on {@code slot}. Idempotent at the network level. */
    public void trigger(Slot slot, BakedAnimation animation) {
        channels[slot.ordinal()] = new AnimationChannel(animation, System.nanoTime(), false);
    }

    /** Clears finished transitions and non-looping non-BASE channels. */
    public void clearFinished(long nowNs) {
        AnimationChannel base = channels[Slot.BASE.ordinal()];
        if (base != null && isTransitionFinished(base.transition(), nowNs)) {
            channels[Slot.BASE.ordinal()] = base.withoutTransition();
        }
        for (Slot slot : Slot.VALUES) {
            if (slot == Slot.BASE) continue;
            if (isFinished(channels[slot.ordinal()], nowNs)) {
                channels[slot.ordinal()] = null;
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
    public void clear(Slot slot) {
        channels[slot.ordinal()] = null;
    }
}
