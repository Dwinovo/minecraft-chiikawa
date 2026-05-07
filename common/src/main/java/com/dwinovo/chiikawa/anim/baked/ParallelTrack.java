package com.dwinovo.chiikawa.anim.baked;

/**
 * One declarative entry in a model's parallel-track sidecar config.
 *
 * <p>Currently carries only the animation name (the same short key used by
 * {@link com.dwinovo.chiikawa.anim.api.AnimationLibrary}). Future fields like
 * {@code condition} (MoLang predicate gating playback), {@code speed}
 * (per-track time multiplier), or {@code weight} (blend strength) will be
 * appended here without breaking the JSON sidecar — the parser already
 * accepts both the string-shorthand form ({@code "blink"}) and the object
 * form ({@code {"animation": "blink"}}), so existing files keep working as
 * fields are added.
 *
 * <p>Records are append-only friendly: adding a parameter means another
 * constructor parameter, but never breaks consumers that don't reference it.
 *
 * @param animation animation short name (e.g. {@code "blink"}, {@code "breath"})
 */
public record ParallelTrack(String animation) {
}
