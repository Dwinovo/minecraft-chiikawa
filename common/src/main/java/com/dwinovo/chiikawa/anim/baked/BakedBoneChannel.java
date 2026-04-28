package com.dwinovo.chiikawa.anim.baked;

/**
 * One animation channel (rotation, position, or scale) for one bone.
 *
 * <p>Storage is data-oriented: keyframes are split into parallel arrays so the
 * sampler can binary-search times once and read 6 floats per keyframe with
 * cache-friendly access. This layout is also {@code repr(C)} friendly for a
 * future Rust port — no boxing, no object pointers in the hot path.
 *
 * <h3>Keyframe layout</h3>
 * Each keyframe stores both a {@code pre} and {@code post} value to handle
 * Bedrock's discontinuity case (a single time stamp with different incoming
 * and outgoing values). When the source JSON only specifies one of the two,
 * the missing side equals the present one. Indices are:
 * <pre>
 *   values[6*i + 0..2] = pre.xyz
 *   values[6*i + 3..5] = post.xyz
 * </pre>
 *
 * <h3>Constant channels</h3>
 * If the source uses the shorthand form {@code "rotation": {"vector": [..]}}
 * with no time keys, the channel is marked {@link #constant}: {@code times}
 * is empty, {@code values} is exactly three floats, and {@code lerpModes}
 * is empty.
 *
 * <h3>Rotations</h3>
 * Rotation values are pre-converted to radians at bake time. Position and
 * scale are stored as-is (pixel units / dimensionless).
 */
public final class BakedBoneChannel {

    public static final byte TYPE_ROTATION = 0;
    public static final byte TYPE_POSITION = 1;
    public static final byte TYPE_SCALE    = 2;

    public static final byte LERP_LINEAR      = 0;
    public static final byte LERP_CATMULL_ROM = 1;
    public static final byte LERP_STEP        = 2;

    public final int boneIdx;
    public final byte channelType;
    public final boolean constant;

    /** Sorted ascending. Length = numKeys (0 for constant channels). */
    public final float[] times;
    /** Length = 6 * numKeys (pre.xyz, post.xyz). For constant channels, length = 3. */
    public final float[] values;
    /** Per-keyframe interpolation mode for the segment starting at this key. Length = numKeys. */
    public final byte[] lerpModes;

    public BakedBoneChannel(int boneIdx, byte channelType, boolean constant,
                            float[] times, float[] values, byte[] lerpModes) {
        this.boneIdx = boneIdx;
        this.channelType = channelType;
        this.constant = constant;
        this.times = times;
        this.values = values;
        this.lerpModes = lerpModes;
    }
}
