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
 */
public final class BakedAnimation {

    public final String name;
    /** Total animation length in seconds. */
    public final float durationSec;
    public final boolean looping;

    public final BakedBoneChannel[] channels;

    public BakedAnimation(String name, float durationSec, boolean looping, BakedBoneChannel[] channels) {
        this.name = name;
        this.durationSec = durationSec;
        this.looping = looping;
        this.channels = channels;
    }
}
