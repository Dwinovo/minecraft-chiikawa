package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;

/**
 * Per-entity animator. Owns the small mutable state needed to drive the
 * pose sampler — a slot table of {@link SlotState} records (main animation +
 * transient triggered animations) and nothing else.
 *
 * <p>Slots are an {@link Slot} enum: {@link Slot#BASE} owns the current
 * looping animation; the upper slots are transient pockets for semantic
 * action, overlay, and reaction events. Adding a new slot is backward-
 * compatible — append to the enum, no array sizing constants to keep in sync.
 *
 * <p>State changes go through {@link #setMain(BakedAnimation, boolean)} or
 * {@link #trigger(Slot, BakedAnimation)} — both replace the slot record
 * wholesale rather than mutating, preserving the pure-function sampling
 * contract.
 */
public final class PetAnimator {

    public static final float DEFAULT_BASE_TRANSITION_SEC = 0.16f;

    /**
     * Stable slot identity.
     *
     * <p>Each slot owns one {@link SlotState}. {@link #BASE} is the looping
     * foundation; non-base slots are transient one-shots reaped by
     * {@link #clearFinished(long)}.
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

    private final SlotState[] slots = new SlotState[Slot.VALUES.length];

    /**
     * Stable per-entity time offset used as the {@code startTimeNs} for parallel
     * tracks (blink, breath, tail idle ...). Initialised lazily from the entity's
     * UUID via {@link #ensureParallelPhase} so two pets created at the same
     * instant don't blink in lockstep. {@link Long#MIN_VALUE} = unset.
     */
    private long parallelPhaseSeed = Long.MIN_VALUE;

    public PetAnimator() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = SlotState.EMPTY;
        }
    }

    /**
     * Latches the entity-stable parallel-phase offset on first call. Subsequent
     * calls are no-ops: the seed must remain stable for the lifetime of the
     * animator to keep the {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler}
     * pure (a moving start time would let two extracts in the same frame
     * produce different animation phases).
     *
     * @param uniquenessSeed any per-entity stable long (typically
     *                       {@code entity.getUUID().getLeastSignificantBits()})
     */
    public void ensureParallelPhase(long uniquenessSeed) {
        if (parallelPhaseSeed != Long.MIN_VALUE) {
            return;
        }
        // Spread blink phase by up to 10 seconds; bigger values would cause
        // animations to "start" in the future relative to nanoTime() and
        // PoseSampler would clamp them to t=0 forever.
        long offsetMs = Math.floorMod(uniquenessSeed, 10_000L);
        parallelPhaseSeed = System.nanoTime() - offsetMs * 1_000_000L;
    }

    /** Returns the latched phase seed. Must call {@link #ensureParallelPhase} first. */
    public long getParallelPhaseSeed() {
        return parallelPhaseSeed;
    }

    /** Returns the slot's state. Never {@code null}; empty slots return {@link SlotState#EMPTY}. */
    public SlotState get(Slot slot) {
        return slots[slot.ordinal()];
    }

    /** Starts a looping main animation if not already playing this exact one. */
    public void setMain(BakedAnimation animation, boolean looping) {
        setMain(animation, looping, DEFAULT_BASE_TRANSITION_SEC);
    }

    /** Starts a main animation, crossfading from the previous main channel when it changes. */
    public void setMain(BakedAnimation animation, boolean looping, float transitionSec) {
        SlotState slot = slots[Slot.BASE.ordinal()];
        AnimationChannel current = slot.current();
        if (current != null && current.animation() == animation && current.looping() == looping) {
            return;
        }
        long nowNs = System.nanoTime();
        AnimationChannel next = new AnimationChannel(animation, nowNs, looping);
        if (current != null && transitionSec > 0f) {
            slots[Slot.BASE.ordinal()] = SlotState.withFade(next, current, nowNs, transitionSec);
        } else {
            slots[Slot.BASE.ordinal()] = SlotState.of(next);
        }
    }

    /** Triggers a non-looping animation on {@code slot}. Idempotent at the network level. */
    public void trigger(Slot slot, BakedAnimation animation) {
        slots[slot.ordinal()] = SlotState.of(new AnimationChannel(animation, System.nanoTime(), false));
    }

    /** Clears finished fades and non-looping non-BASE channels. */
    public void clearFinished(long nowNs) {
        SlotState base = slots[Slot.BASE.ordinal()];
        if (base.hasFade() && isFadeFinished(base, nowNs)) {
            slots[Slot.BASE.ordinal()] = base.withoutFade();
        }
        for (Slot slot : Slot.VALUES) {
            if (slot == Slot.BASE) continue;
            SlotState state = slots[slot.ordinal()];
            if (isFinished(state.current(), nowNs)) {
                slots[slot.ordinal()] = SlotState.EMPTY;
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

    /** Returns whether the slot's crossfade has reached its configured duration. */
    public static boolean isFadeFinished(SlotState slot, long nowNs) {
        if (slot == null || !slot.hasFade()) {
            return false;
        }
        if (slot.fadeDurationSec() <= 0f) {
            return true;
        }
        long elapsedNs = nowNs - slot.fadeStartNs();
        return elapsedNs >= (long) (slot.fadeDurationSec() * 1_000_000_000L);
    }

    /** Clears the slot — used when a one-shot finishes. */
    public void clear(Slot slot) {
        slots[slot.ordinal()] = SlotState.EMPTY;
    }
}
