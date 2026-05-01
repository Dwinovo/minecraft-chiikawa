package com.dwinovo.chiikawa.anim.runtime;

/**
 * Lightweight transition metadata for a channel switch.
 *
 * <p>The transition is generic: it does not know whether the switch is
 * idle-to-run, run-to-sit, or any other gameplay state. It only remembers the
 * previous channel and how long the pose mixer should crossfade into the new
 * channel.
 *
 * @param fromChannel channel to blend out from
 * @param startTimeNs {@code System.nanoTime()} captured when the switch began
 * @param durationSec blend duration in seconds
 */
public record AnimationTransition(
        AnimationChannel fromChannel,
        long startTimeNs,
        float durationSec
) {
}
