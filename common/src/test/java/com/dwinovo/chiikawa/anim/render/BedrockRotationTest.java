package com.dwinovo.chiikawa.anim.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BedrockRotationTest {
    private static final float DEG = (float) (Math.PI / 180.0);

    @Test
    void aHeadLookingAsideAndUpStaysLevel() {
        // As the head-look sets it: pitched up 30 degrees, turned 60 to the side.
        Quaternionf turn = BedrockRotation.of(new Quaternionf(), -30 * DEG, -60 * DEG, 0);
        Vector3f across = turn.transform(new Vector3f(1, 0, 0));
        assertEquals(0.0f, across.y, 1.0e-5f, "the head's side-to-side axis tipped over: it lolls");
    }

    @Test
    void xIsTurnedFirstThenYThenZ() {
        float x = 20 * DEG, y = 35 * DEG, z = -15 * DEG;
        Quaternionf expected = new Quaternionf().rotateZ(z).rotateY(y).rotateX(x);
        Vector3f point = new Vector3f(0.3f, 0.7f, -0.5f);
        Vector3f want = expected.transform(new Vector3f(point));
        Vector3f got = BedrockRotation.of(new Quaternionf(), x, y, z).transform(new Vector3f(point));
        assertEquals(want.x, got.x, 1.0e-5f);
        assertEquals(want.y, got.y, 1.0e-5f);
        assertEquals(want.z, got.z, 1.0e-5f);
    }

    @Test
    void theAnglesComeBackAsTheyWent() {
        float[][] cases = {{-20, -40, 0}, {35, 70, -15}, {-60, 10, 45}, {89, -30, 20}, {0, 0, 0}};
        for (float[] c : cases) {
            Quaternionf turn = BedrockRotation.of(new Quaternionf(), c[0] * DEG, c[1] * DEG, c[2] * DEG);
            Vector3f back = BedrockRotation.angles(turn, new Vector3f());
            Quaternionf again = BedrockRotation.of(new Quaternionf(), back.x, back.y, back.z);
            Vector3f point = new Vector3f(0.3f, 0.7f, -0.5f);
            Vector3f a = turn.transform(new Vector3f(point));
            Vector3f b = again.transform(new Vector3f(point));
            assertEquals(a.x, b.x, 1.0e-4f, () -> java.util.Arrays.toString(c));
            assertEquals(a.y, b.y, 1.0e-4f, () -> java.util.Arrays.toString(c));
            assertEquals(a.z, b.z, 1.0e-4f, () -> java.util.Arrays.toString(c));
        }
    }
}
