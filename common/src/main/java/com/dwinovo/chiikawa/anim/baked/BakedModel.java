package com.dwinovo.chiikawa.anim.baked;

import java.util.Map;

/**
 * Immutable, render-ready model. Shared across all entity instances of the
 * same type — never mutated after baking.
 *
 * Bones are stored in BFS order, so iterating them once renders the entire
 * skeleton with parent transforms always seen before children.
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

    public BakedModel(BakedBone[] bones, BakedCube[] cubes, int[] rootBones,
                      Map<String, Integer> boneIndex,
                      int textureWidth, int textureHeight) {
        this.bones = bones;
        this.cubes = cubes;
        this.rootBones = rootBones;
        this.boneIndex = boneIndex;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }
}
