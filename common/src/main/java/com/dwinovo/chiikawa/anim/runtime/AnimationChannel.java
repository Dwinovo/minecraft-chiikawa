package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;

/**
 * A single playing animation slot on an entity. Immutable — to start a new
 * animation, replace the whole record rather than mutating fields.
 *
 * <p>This immutability is the core fix for double-sampling bugs: there is no
 * mutable "last age" field that gets advanced on every render-state extract.
 * The current pose is always derived as a pure function of {@code (animation,
 * startTimeNs, nowNs)}, so calling {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler}
 * twice in the same frame produces identical results — GUI preview and world
 * render naturally agree.
 *
 * @param animation   the baked animation being played
 * @param startTimeNs {@code System.nanoTime()} captured at trigger
 * @param looping     whether to wrap {@code (now - start) % duration}
 * @param transition  optional transition blended into this channel
 */
public record AnimationChannel(
        BakedAnimation animation,
        long startTimeNs,
        boolean looping,
        AnimationTransition transition
) {
    public AnimationChannel(BakedAnimation animation, long startTimeNs, boolean looping) {
        this(animation, startTimeNs, looping, null);
    }

    public AnimationChannel withoutTransition() {
        if (transition == null) {
            return this;
        }
        return new AnimationChannel(animation, startTimeNs, looping);
    }
}
