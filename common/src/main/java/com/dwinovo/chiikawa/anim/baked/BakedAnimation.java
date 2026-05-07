package com.dwinovo.chiikawa.anim.baked;

/**
 * Immutable, render-ready animation. Shared across all entities playing the
 * same animation — never mutated after baking.
 *
 * <p>Channels are flat-arrayed in iteration order. Each {@link BakedBoneChannel}
 * carries the index of the bone it targets in the model's bone array, so the
 * sampler can write directly into a pose buffer indexed by that same array.
 *
 * <p>The mapping from a {@link BakedAnimation} to a specific {@link BakedModel}
 * happens at sample time via the bone-index field on each channel. An
 * animation only references bone names that exist in the model — channels for
 * unknown names are dropped at bake time (with a warning).
 *
 * <p>{@link #bakeStamp} matches the stamp on the {@link BakedModel} this
 * animation was baked against. After a resource reload the stamp on
 * {@link BakeStamp} is bumped and fresh objects are produced; any cached
 * reference compares unequal and can be detected as stale before its
 * out-of-date bone indices reach the sampler.
 */
public final class BakedAnimation {

    public final String name;
    /** Total animation length in seconds. */
    public final float durationSec;
    public final boolean looping;

    public final BakedBoneChannel[] channels;

    /** {@link BakeStamp} value at the moment this animation was baked. */
    public final long bakeStamp;

    /** Test-only: defaults {@link #bakeStamp} to {@code 0} (unset). */
    public BakedAnimation(String name, float durationSec, boolean looping, BakedBoneChannel[] channels) {
        this(name, durationSec, looping, channels, 0L);
    }

    public BakedAnimation(String name, float durationSec, boolean looping,
                          BakedBoneChannel[] channels, long bakeStamp) {
        this.name = name;
        this.durationSec = durationSec;
        this.looping = looping;
        this.channels = channels;
        this.bakeStamp = bakeStamp;
    }
}
