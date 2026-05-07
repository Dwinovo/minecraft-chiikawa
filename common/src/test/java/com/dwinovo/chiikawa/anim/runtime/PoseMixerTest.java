package com.dwinovo.chiikawa.anim.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoseMixerTest {

    @Test
    void fadeAlphaUsesSmoothstep() {
        long startNs = 1_000_000_000L;
        float durationSec = 1.0f;

        assertEquals(0.0f, PoseMixer.fadeAlpha(startNs, durationSec, 1_000_000_000L));
        assertEquals(0.5f, PoseMixer.fadeAlpha(startNs, durationSec, 1_500_000_000L));
        assertEquals(1.0f, PoseMixer.fadeAlpha(startNs, durationSec, 2_000_000_000L));
    }

    @Test
    void blendUsesShortestRotationPath() {
        float[] from = oneBonePose();
        float[] to = oneBonePose();
        float[] out = oneBonePose();
        from[PoseSampler.OFFSET_ROT + 2] = (float) Math.toRadians(179.0);
        to[PoseSampler.OFFSET_ROT + 2] = (float) Math.toRadians(-179.0);

        PoseMixer.blend(from, to, 0.5f, out, 1);

        assertEquals(Math.PI, out[PoseSampler.OFFSET_ROT + 2], 0.0001);
    }

    @Test
    void blendInterpolatesPositionAndScaleLinearly() {
        float[] from = oneBonePose();
        float[] to = oneBonePose();
        float[] out = oneBonePose();
        from[PoseSampler.OFFSET_POS] = 2f;
        to[PoseSampler.OFFSET_POS] = 10f;
        from[PoseSampler.OFFSET_SCALE] = 1f;
        to[PoseSampler.OFFSET_SCALE] = 3f;

        PoseMixer.blend(from, to, 0.25f, out, 1);

        assertEquals(4f, out[PoseSampler.OFFSET_POS]);
        assertEquals(1.5f, out[PoseSampler.OFFSET_SCALE]);
    }

    private static float[] oneBonePose() {
        float[] pose = new float[PoseSampler.FLOATS_PER_BONE];
        pose[PoseSampler.OFFSET_SCALE] = 1f;
        pose[PoseSampler.OFFSET_SCALE + 1] = 1f;
        pose[PoseSampler.OFFSET_SCALE + 2] = 1f;
        return pose;
    }
}
