package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;

/**
 * Pure-function pose sampler. The core of solving GeckoLib issue #848:
 * given an animation, a per-channel start time, and {@code System.nanoTime()},
 * produces the exact pose for that instant. Calling this twice in the same
 * frame returns identical output — no mutable {@code lastAnimatableAge}
 * counter to advance, so {@code InventoryScreen}'s second
 * {@code extractRenderState} call cannot accidentally double-step animation.
 *
 * <h3>Pose buffer layout</h3>
 * Caller-owned, reused across frames. Per bone:
 * <pre>
 *   [b * 9 + 0..2] rotation delta  (radians, XYZ Euler, applied on top of rest pose)
 *   [b * 9 + 3..5] position offset (pixel units, applied as a translate)
 *   [b * 9 + 6..8] scale           (multiplier; identity = 1)
 * </pre>
 * Use {@link #resetIdentity} before each frame to clear stale data.
 *
 * <h3>Interpolation</h3>
 * Linear and Catmull-Rom (uniform, 4 control points with end clamping). Step
 * mode is honored as a hold to {@code post[i]}. Edge cases ({@code t}
 * outside {@code [times[0], times[n-1]]}) clamp to the boundary
 * {@code pre} / {@code post} values.
 */
public final class PoseSampler {

    public static final int FLOATS_PER_BONE = 9;
    public static final int OFFSET_ROT   = 0;
    public static final int OFFSET_POS   = 3;
    public static final int OFFSET_SCALE = 6;

    private PoseSampler() {}

    /**
     * Writes identity pose (rotation 0, position 0, scale 1) into every bone
     * slot of {@code poseBuf}. Caller is responsible for sizing {@code poseBuf}
     * to at least {@code boneCount * FLOATS_PER_BONE}.
     */
    public static void resetIdentity(float[] poseBuf, int boneCount) {
        for (int b = 0; b < boneCount; b++) {
            int base = b * FLOATS_PER_BONE;
            poseBuf[base]     = 0f;  poseBuf[base + 1] = 0f;  poseBuf[base + 2] = 0f;
            poseBuf[base + 3] = 0f;  poseBuf[base + 4] = 0f;  poseBuf[base + 5] = 0f;
            poseBuf[base + 6] = 1f;  poseBuf[base + 7] = 1f;  poseBuf[base + 8] = 1f;
        }
    }

    /** Samples one channel into {@code poseBuf}. {@code null} channels are skipped. */
    public static void sample(AnimationChannel channel, long nowNs, float[] poseBuf) {
        if (channel == null) return;
        BakedAnimation anim = channel.animation();
        if (anim == null) return;
        float t = computeAnimTime(channel, nowNs);
        for (BakedBoneChannel ch : anim.channels) {
            applyChannel(ch, t, poseBuf);
        }
    }

    /** Computes the local animation time in seconds for {@code channel} at {@code nowNs}. */
    public static float computeAnimTime(AnimationChannel channel, long nowNs) {
        BakedAnimation anim = channel.animation();
        float elapsed = (float) ((nowNs - channel.startTimeNs()) / 1.0e9);
        if (elapsed < 0f) elapsed = 0f;
        if (anim.durationSec <= 0f) return 0f;
        if (channel.looping()) {
            elapsed = elapsed % anim.durationSec;
            if (elapsed < 0f) elapsed += anim.durationSec;
        } else if (elapsed > anim.durationSec) {
            elapsed = anim.durationSec;
        }
        return elapsed;
    }

    private static void applyChannel(BakedBoneChannel ch, float t, float[] poseBuf) {
        int base = ch.boneIdx * FLOATS_PER_BONE + offsetFor(ch.channelType);
        if (ch.constant) {
            poseBuf[base]     = ch.values[0];
            poseBuf[base + 1] = ch.values[1];
            poseBuf[base + 2] = ch.values[2];
            return;
        }
        sampleKeyframed(ch, t, poseBuf, base);
    }

    private static int offsetFor(byte type) {
        return switch (type) {
            case BakedBoneChannel.TYPE_ROTATION -> OFFSET_ROT;
            case BakedBoneChannel.TYPE_POSITION -> OFFSET_POS;
            case BakedBoneChannel.TYPE_SCALE   -> OFFSET_SCALE;
            default -> 0;
        };
    }

    private static void sampleKeyframed(BakedBoneChannel ch, float t, float[] poseBuf, int base) {
        float[] times = ch.times;
        float[] values = ch.values;
        int n = times.length;
        if (n == 0) return;

        if (t <= times[0]) {
            // Before first key — use its pre.
            poseBuf[base]     = values[0];
            poseBuf[base + 1] = values[1];
            poseBuf[base + 2] = values[2];
            return;
        }
        if (t >= times[n - 1]) {
            // After last key — use its post.
            int last = (n - 1) * 6;
            poseBuf[base]     = values[last + 3];
            poseBuf[base + 1] = values[last + 4];
            poseBuf[base + 2] = values[last + 5];
            return;
        }

        int i = binarySearchLE(times, t);
        if (i >= n - 1) i = n - 2;
        float t0 = times[i];
        float t1 = times[i + 1];
        float dt = t1 - t0;
        float u = dt > 1.0e-6f ? (t - t0) / dt : 0f;
        if (u < 0f) u = 0f; else if (u > 1f) u = 1f;

        byte lerp = ch.lerpModes[i];
        int p1Base = i * 6 + 3;        // post[i]
        int p2Base = (i + 1) * 6;      // pre[i+1]

        if (lerp == BakedBoneChannel.LERP_STEP) {
            poseBuf[base]     = values[p1Base];
            poseBuf[base + 1] = values[p1Base + 1];
            poseBuf[base + 2] = values[p1Base + 2];
            return;
        }

        if (lerp == BakedBoneChannel.LERP_CATMULL_ROM) {
            // Edge clamping: P0 = post[i-1] (or post[i] at start), P3 = pre[i+2] (or pre[i+1] at end).
            int p0Base = i > 0 ? (i - 1) * 6 + 3 : p1Base;
            int p3Base = (i + 2 < n) ? (i + 2) * 6 : p2Base;
            for (int axis = 0; axis < 3; axis++) {
                poseBuf[base + axis] = catmullRom(
                        values[p0Base + axis],
                        values[p1Base + axis],
                        values[p2Base + axis],
                        values[p3Base + axis],
                        u);
            }
            return;
        }

        // Linear (default).
        for (int axis = 0; axis < 3; axis++) {
            float a = values[p1Base + axis];
            float b = values[p2Base + axis];
            poseBuf[base + axis] = a + (b - a) * u;
        }
    }

    /**
     * Uniform Catmull-Rom basis: passes through P1 at u=0 and P2 at u=1.
     * Tangents use neighbors P0 and P3.
     */
    private static float catmullRom(float p0, float p1, float p2, float p3, float u) {
        float u2 = u * u;
        float u3 = u2 * u;
        return 0.5f * (
                (2f * p1)
                        + (-p0 + p2) * u
                        + (2f * p0 - 5f * p1 + 4f * p2 - p3) * u2
                        + (-p0 + 3f * p1 - 3f * p2 + p3) * u3
        );
    }

    /** Returns the largest {@code i} with {@code arr[i] <= t}. Caller ensures {@code t} is in range. */
    private static int binarySearchLE(float[] arr, float t) {
        int lo = 0;
        int hi = arr.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (arr[mid] <= t) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }
}
