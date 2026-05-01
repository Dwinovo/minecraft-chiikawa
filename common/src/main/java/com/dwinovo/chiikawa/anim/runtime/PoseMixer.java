package com.dwinovo.chiikawa.anim.runtime;

/**
 * Pose-level blending helpers. This stays below the state machine: callers
 * decide which channels to sample, while the mixer only blends numeric pose
 * buffers.
 */
public final class PoseMixer {

    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2.0f;

    private PoseMixer() {
    }

    public static float transitionAlpha(AnimationTransition transition, long nowNs) {
        if (transition == null || transition.durationSec() <= 0f) {
            return 1f;
        }
        float elapsed = (float) ((nowNs - transition.startTimeNs()) / 1.0e9);
        float t = clamp01(elapsed / transition.durationSec());
        return smoothstep(t);
    }

    public static void blend(float[] fromPose, float[] toPose, float alpha, float[] outPose, int boneCount) {
        float t = clamp01(alpha);
        for (int bone = 0; bone < boneCount; bone++) {
            int base = bone * PoseSampler.FLOATS_PER_BONE;
            for (int axis = 0; axis < 3; axis++) {
                int idx = base + PoseSampler.OFFSET_ROT + axis;
                outPose[idx] = lerpAngleRadians(fromPose[idx], toPose[idx], t);
            }
            for (int axis = 0; axis < 3; axis++) {
                int idx = base + PoseSampler.OFFSET_POS + axis;
                outPose[idx] = lerp(fromPose[idx], toPose[idx], t);
            }
            for (int axis = 0; axis < 3; axis++) {
                int idx = base + PoseSampler.OFFSET_SCALE + axis;
                outPose[idx] = lerp(fromPose[idx], toPose[idx], t);
            }
        }
    }

    public static float smoothstep(float t) {
        float x = clamp01(t);
        return x * x * (3f - 2f * x);
    }

    private static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    private static float lerpAngleRadians(float from, float to, float alpha) {
        return from + wrapRadians(to - from) * alpha;
    }

    private static float wrapRadians(float radians) {
        float wrapped = radians % TAU;
        if (wrapped <= -PI) {
            wrapped += TAU;
        } else if (wrapped > PI) {
            wrapped -= TAU;
        }
        return wrapped;
    }

    private static float clamp01(float value) {
        if (value <= 0f) {
            return 0f;
        }
        if (value >= 1f) {
            return 1f;
        }
        return value;
    }
}
