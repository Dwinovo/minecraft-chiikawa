package com.dwinovo.chiikawa.anim.runtime;

/**
 * Per-slot animation state — the {@link AnimationChannel} that is currently
 * playing on a slot, plus optional crossfade metadata into the channel that
 * was playing before.
 *
 * <p>Lifting fade timing into the slot (rather than embedding it inside the
 * {@link AnimationChannel} record) makes the channel itself purely "what's
 * playing now / from when / loop?", while the slot owns "what am I fading
 * away from, and how long does the fade last?". That separation means every
 * slot can in principle support its own crossfade — the renderer no longer
 * has to special-case the BASE slot the way the previous design did.
 *
 * <p>Slots are immutable records; transitions are expressed by replacing the
 * whole record. {@link #EMPTY} is the canonical "nothing playing" sentinel
 * to keep null-checks out of the hot path.
 *
 * @param current          channel that is the slot's authoritative playing animation, or {@code null} if empty
 * @param previous         channel being faded out from, or {@code null} when no fade is in progress
 * @param fadeStartNs      {@code System.nanoTime()} captured when the fade began
 * @param fadeDurationSec  fade length in seconds; {@code 0f} means "snap immediately to current"
 */
public record SlotState(
        AnimationChannel current,
        AnimationChannel previous,
        long fadeStartNs,
        float fadeDurationSec
) {

    /** Canonical "nothing playing" sentinel; safe to share across slots. */
    public static final SlotState EMPTY = new SlotState(null, null, 0L, 0f);

    /** Slot with a single channel and no fade in progress. */
    public static SlotState of(AnimationChannel current) {
        return new SlotState(current, null, 0L, 0f);
    }

    /** Slot crossfading from {@code previous} into {@code current} over {@code fadeDurationSec}. */
    public static SlotState withFade(AnimationChannel current, AnimationChannel previous,
                                      long fadeStartNs, float fadeDurationSec) {
        return new SlotState(current, previous, fadeStartNs, fadeDurationSec);
    }

    public boolean isEmpty() {
        return current == null;
    }

    public boolean hasFade() {
        return previous != null;
    }

    /** Returns a copy with the fade cleared, preserving {@link #current}. */
    public SlotState withoutFade() {
        if (previous == null) return this;
        return new SlotState(current, null, 0L, 0f);
    }
}
