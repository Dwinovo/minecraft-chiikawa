package com.dwinovo.chiikawa.anim.runtime;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.molang.MolangNode;

/**
 * Pure-function pose sampler. The core of solving double-sampling bugs:
 * given an animation, a per-channel start time, and {@code System.nanoTime()},
 * produces the exact pose for that instant. Calling this twice in the same
 * frame returns identical output — no mutable {@code lastAnimatableAge}
 * counter to advance, so {@code InventoryScreen}'s second
 * {@code extractRenderState} call cannot accidentally double-step animation.
 *
 * <h2>Pose buffer layout</h2>
 * Caller-owned, reused across frames. Per bone:
 * <pre>
 *   [b * 9 + 0..2] rotation delta  (radians, XYZ Euler, applied on top of rest pose)
 *   [b * 9 + 3..5] position offset (pixel units, applied as a translate)
 *   [b * 9 + 6..8] scale           (multiplier; identity = 1)
 * </pre>
 * Use {@link #resetIdentity} before each frame to clear stale data.
 *
 * <h2>Molang slots</h2>
 * Channels carry an optional parallel {@link MolangNode} array. When a slot
 * is non-null, it is evaluated against the supplied {@link MolangContext}
 * and overrides the numeric value at that slot. The compiler has already
 * wrapped each node so its output includes the same X-mirror / unit
 * conversion as the numeric path — no special-case branching here.
 */
public final class PoseSampler {

    public static final int FLOATS_PER_BONE = 9;
    public static final int OFFSET_ROT   = 0;
    public static final int OFFSET_POS   = 3;
    public static final int OFFSET_SCALE = 6;

    private PoseSampler() {}

    public static void resetIdentity(float[] poseBuf, int boneCount) {
        for (int b = 0; b < boneCount; b++) {
            int base = b * FLOATS_PER_BONE;
            poseBuf[base]     = 0f;  poseBuf[base + 1] = 0f;  poseBuf[base + 2] = 0f;
            poseBuf[base + 3] = 0f;  poseBuf[base + 4] = 0f;  poseBuf[base + 5] = 0f;
            poseBuf[base + 6] = 1f;  poseBuf[base + 7] = 1f;  poseBuf[base + 8] = 1f;
        }
    }

    /**
     * Samples one channel into {@code poseBuf}. {@code null} channels are
     * skipped. Updates {@code ctx.vars[SLOT_ANIM_TIME]} so any Molang nodes
     * referencing {@code query.anim_time} see this channel's local time.
     */
    public static void sample(AnimationChannel channel, long nowNs,
                              MolangContext ctx, float[] poseBuf) {
        if (channel == null) return;
        BakedAnimation anim = channel.animation();
        if (anim == null) return;
        float t = computeAnimTime(channel, nowNs);
        ctx.vars[MolangContext.SLOT_ANIM_TIME] = t;
        for (BakedBoneChannel ch : anim.channels) {
            applyChannel(ch, t, ctx, poseBuf);
        }
    }

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

    private static void applyChannel(BakedBoneChannel ch, float t, MolangContext ctx, float[] poseBuf) {
        int base = ch.boneIdx * FLOATS_PER_BONE + offsetFor(ch.channelType);
        if (ch.constant) {
            poseBuf[base]     = readSlot(ch, 0, ctx);
            poseBuf[base + 1] = readSlot(ch, 1, ctx);
            poseBuf[base + 2] = readSlot(ch, 2, ctx);
            return;
        }
        sampleKeyframed(ch, t, ctx, poseBuf, base);
    }

    private static int offsetFor(byte type) {
        return switch (type) {
            case BakedBoneChannel.TYPE_ROTATION -> OFFSET_ROT;
            case BakedBoneChannel.TYPE_POSITION -> OFFSET_POS;
            case BakedBoneChannel.TYPE_SCALE   -> OFFSET_SCALE;
            default -> 0;
        };
    }

    /** Returns slot {@code i}'s value, evaluating its Molang node if one is bound. */
    private static float readSlot(BakedBoneChannel ch, int slotIdx, MolangContext ctx) {
        MolangNode[] slots = ch.molangSlots;
        if (slots != null && slotIdx < slots.length && slots[slotIdx] != null) {
            return (float) slots[slotIdx].eval(ctx);
        }
        return ch.values[slotIdx];
    }

    private static void sampleKeyframed(BakedBoneChannel ch, float t, MolangContext ctx,
                                        float[] poseBuf, int base) {
        float[] times = ch.times;
        int n = times.length;
        if (n == 0) return;

        if (t <= times[0]) {
            // Before first key — use its pre.
            poseBuf[base]     = readSlot(ch, 0, ctx);
            poseBuf[base + 1] = readSlot(ch, 1, ctx);
            poseBuf[base + 2] = readSlot(ch, 2, ctx);
            return;
        }
        if (t >= times[n - 1]) {
            int last = (n - 1) * 6;
            poseBuf[base]     = readSlot(ch, last + 3, ctx);
            poseBuf[base + 1] = readSlot(ch, last + 4, ctx);
            poseBuf[base + 2] = readSlot(ch, last + 5, ctx);
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
            poseBuf[base]     = readSlot(ch, p1Base,     ctx);
            poseBuf[base + 1] = readSlot(ch, p1Base + 1, ctx);
            poseBuf[base + 2] = readSlot(ch, p1Base + 2, ctx);
            return;
        }

        if (lerp == BakedBoneChannel.LERP_CATMULL_ROM) {
            int p0Base = i > 0 ? (i - 1) * 6 + 3 : p1Base;
            int p3Base = (i + 2 < n) ? (i + 2) * 6 : p2Base;
            for (int axis = 0; axis < 3; axis++) {
                poseBuf[base + axis] = catmullRom(
                        readSlot(ch, p0Base + axis, ctx),
                        readSlot(ch, p1Base + axis, ctx),
                        readSlot(ch, p2Base + axis, ctx),
                        readSlot(ch, p3Base + axis, ctx),
                        u);
            }
            return;
        }

        // Linear (default).
        for (int axis = 0; axis < 3; axis++) {
            float a = readSlot(ch, p1Base + axis, ctx);
            float b = readSlot(ch, p2Base + axis, ctx);
            poseBuf[base + axis] = a + (b - a) * u;
        }
    }

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
