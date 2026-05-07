package com.dwinovo.chiikawa.anim.baked;

import java.util.List;
import java.util.Map;

/**
 * Immutable, render-ready model. Shared across all entity instances of the
 * same type — never mutated after baking.
 *
 * Bones are stored in BFS order, so iterating them once renders the entire
 * skeleton with parent transforms always seen before children.
 *
 * <h2>Parallel tracks</h2>
 * Animations declared by the model's sidecar config (e.g.
 * {@code assets/<ns>/parallel/<pet>.json}) play continuously alongside the
 * BASE / sub-slot channels. Stored here as a list of animation short names
 * (resolved against {@link com.dwinovo.chiikawa.anim.api.AnimationLibrary} per
 * frame, same way as the base loop). Empty when the pet has no parallel
 * declaration.
 */
public final class BakedModel {

    public final BakedBone[] bones;
    public final BakedCube[] cubes;
    /** Indices of bones with no parent (entry points for DFS render). */
    public final int[] rootBones;
    /** Bone name → index lookup. Used by animation channels at bake time. */
    public final Map<String, Integer> boneIndex;

    public final int textureWidth;
    public final int textureHeight;

    /**
     * Animation short names that should play in parallel with the main slots.
     * Empty (never {@code null}) when no parallel sidecar is declared.
     */
    public final List<String> parallelTracks;

    public BakedModel(BakedBone[] bones, BakedCube[] cubes, int[] rootBones,
                      Map<String, Integer> boneIndex,
                      int textureWidth, int textureHeight,
                      List<String> parallelTracks) {
        this.bones = bones;
        this.cubes = cubes;
        this.rootBones = rootBones;
        this.boneIndex = boneIndex;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.parallelTracks = parallelTracks == null ? List.of() : List.copyOf(parallelTracks);
    }
}
