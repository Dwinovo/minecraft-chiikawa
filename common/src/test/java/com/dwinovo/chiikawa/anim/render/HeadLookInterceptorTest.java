package com.dwinovo.chiikawa.anim.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class HeadLookInterceptorTest {
    private static final float DEG = (float) (Math.PI / 180.0);

    /** Root, a body above it and the head on the body, as the pets are built. */
    private static BakedModel skeleton() {
        BakedBone[] bones = {
            new BakedBone("Root", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[] {1}),
            new BakedBone("UpBody", 0, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[] {2}),
            new BakedBone("AllHead", 1, 0, 9, 0, 0, 0, 0, false, 0, 0, new int[0]),
        };
        return new BakedModel(bones, new BakedCube[0], new int[] {0},
            Map.of("Root", 0, "UpBody", 1, "AllHead", 2), 64, 64);
    }

    private static float[] pose(float bodyPitchDeg, float headPitchDeg) {
        float[] buf = new float[3 * PoseSampler.FLOATS_PER_BONE];
        for (int b = 0; b < 3; b++) {
            buf[b * PoseSampler.FLOATS_PER_BONE + 6] = 1;
            buf[b * PoseSampler.FLOATS_PER_BONE + 7] = 1;
            buf[b * PoseSampler.FLOATS_PER_BONE + 8] = 1;
        }
        buf[PoseSampler.FLOATS_PER_BONE] = bodyPitchDeg * DEG;
        buf[2 * PoseSampler.FLOATS_PER_BONE] = headPitchDeg * DEG;
        return buf;
    }

    /** The head's turn in the model, from its bone and the bones above it. */
    private static Quaternionf headInModel(float[] buf) {
        Quaternionf world = new Quaternionf();
        for (int b = 0; b < 3; b++) {
            int at = b * PoseSampler.FLOATS_PER_BONE;
            world.mul(BedrockRotation.of(new Quaternionf(), buf[at], buf[at + 1], buf[at + 2]));
        }
        return world;
    }

    private static ChiikawaRenderState looking(float yaw, float pitch) {
        ChiikawaRenderState state = new ChiikawaRenderState();
        state.netHeadYaw = yaw;
        state.headPitch = pitch;
        return state;
    }

    @Test
    void aSittingPetLookingAsideAndUpKeepsItsHeadLevel() {
        // Sitting: the body leans forward 15 degrees and the head leans back 15 to stay upright.
        float[] buf = pose(15, -15);
        new HeadLookInterceptor().apply(skeleton(), looking(60, -30), null, buf);
        Vector3f across = headInModel(buf).transform(new Vector3f(1, 0, 0));
        assertEquals(0.0f, across.y, 1.0e-4f, "the head rolled over as the leaning body turned it");
    }

    @Test
    void theHeadTurnsAsTheLookSaysWhateverTheBodyDoes() {
        float[] buf = pose(15, -15);
        new HeadLookInterceptor().apply(skeleton(), looking(60, -30), null, buf);
        Quaternionf want = BedrockRotation.of(new Quaternionf(), 30 * DEG, -60 * DEG, 0);
        Vector3f forward = headInModel(buf).transform(new Vector3f(0, 0, -1));
        Vector3f wanted = want.transform(new Vector3f(0, 0, -1));
        assertEquals(wanted.x, forward.x, 1.0e-4f);
        assertEquals(wanted.y, forward.y, 1.0e-4f);
        assertEquals(wanted.z, forward.z, 1.0e-4f);
    }

    @Test
    void standingStraightTheHeadTakesTheLookAsItIs() {
        float[] buf = pose(0, 0);
        new HeadLookInterceptor().apply(skeleton(), looking(40, 20), null, buf);
        int head = 2 * PoseSampler.FLOATS_PER_BONE;
        assertEquals(-20 * DEG, buf[head], 1.0e-4f);
        assertEquals(-40 * DEG, buf[head + 1], 1.0e-4f);
        assertEquals(0.0f, buf[head + 2], 1.0e-4f);
    }
}
