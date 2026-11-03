package com.dwinovo.chiikawa.anim.compile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

/** Bedrock lets a single number or Molang string stand where a vector is expected: it means all three axes. */
class AnimationBakerTest {

    private static BakedModel oneBone() {
        BakedBone[] bones = {new BakedBone("Eyes", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0])};
        return new BakedModel(bones, new BakedCube[0], new int[] {0}, Map.of("Eyes", 0), 64, 64);
    }

    /** The channels baked from one animation that gives {@code Eyes} the channel {@code body}, e.g. {@code "scale": 0}. */
    private static BakedBoneChannel[] bake(String body) {
        JsonObject root = JsonParser.parseString(
            "{\"animations\": {\"a\": {\"animation_length\": 1, \"bones\": {\"Eyes\": {" + body + "}}}}}").getAsJsonObject();
        BakedAnimation animation = AnimationBaker.bake(root, oneBone()).get("a");
        assertNotNull(animation);
        return animation.channels;
    }

    private static BakedBoneChannel only(String body) {
        BakedBoneChannel[] channels = bake(body);
        assertEquals(1, channels.length, body);
        return channels[0];
    }

    /** The scalar spelling bakes to what its array spelling does. */
    private static void assertSame(String scalar, String array) {
        BakedBoneChannel s = only(scalar);
        BakedBoneChannel a = only(array);
        assertEquals(a.channelType, s.channelType, scalar);
        assertEquals(a.constant, s.constant, scalar);
        assertArrayEquals(a.times, s.times, scalar);
        assertArrayEquals(a.values, s.values, scalar);
        assertArrayEquals(a.lerpModes, s.lerpModes, scalar);
        assertNull(s.molangSlots, scalar);
    }

    @Test
    void aConstantChannelGivenAsAScalarBakesAsItsArray() {
        assertSame("\"scale\": 0", "\"scale\": [0, 0, 0]");
        assertSame("\"position\": 2", "\"position\": [2, 2, 2]");
        assertSame("\"rotation\": 90", "\"rotation\": [90, 90, 90]");
        assertSame("\"scale\": {\"vector\": 0.5}", "\"scale\": {\"vector\": [0.5, 0.5, 0.5]}");
        assertSame("\"scale\": \"0.5\"", "\"scale\": [0.5, 0.5, 0.5]");
    }

    @Test
    void aKeyframeGivenAsAScalarBakesAsItsArray() {
        assertSame("\"scale\": {\"0\": 1, \"0.5\": 0, \"1\": 1}",
            "\"scale\": {\"0\": [1, 1, 1], \"0.5\": [0, 0, 0], \"1\": [1, 1, 1]}");
        assertSame("\"rotation\": {\"0\": 10, \"1\": {\"vector\": 20}}",
            "\"rotation\": {\"0\": [10, 10, 10], \"1\": {\"vector\": [20, 20, 20]}}");
    }

    @Test
    void aPreAndAPostGivenAsScalarsBakeAsTheirArrays() {
        assertSame("\"scale\": {\"0\": 1, \"0.5\": {\"pre\": 1, \"post\": 0}, \"1\": 0}",
            "\"scale\": {\"0\": [1, 1, 1], \"0.5\": {\"pre\": [1, 1, 1], \"post\": [0, 0, 0]}, \"1\": [0, 0, 0]}");
        assertSame("\"scale\": {\"0\": 1, \"0.5\": {\"post\": 0}, \"1\": 1}",
            "\"scale\": {\"0\": [1, 1, 1], \"0.5\": {\"post\": [0, 0, 0]}, \"1\": [1, 1, 1]}");
        assertSame("\"scale\": {\"0\": 1, \"0.5\": {\"pre\": {\"vector\": 0}}, \"1\": 1}",
            "\"scale\": {\"0\": [1, 1, 1], \"0.5\": {\"pre\": {\"vector\": [0, 0, 0]}}, \"1\": [1, 1, 1]}");
    }

    @Test
    void aScalarMolangStringIsCompiledForAllThreeAxes() {
        MolangContext ctx = new MolangContext();
        ctx.vars[MolangContext.SLOT_ANIM_TIME] = 3.0;

        BakedBoneChannel constant = only("\"position\": \"query.anim_time * 2\"");
        assertNotNull(constant.molangSlots);
        assertEquals(3, constant.molangSlots.length);
        // Position X is mirrored at bake time, so the shared expression reads -6 there and 6 on the others.
        assertEquals(-6.0, constant.molangSlots[0].eval(ctx), 1e-9);
        assertEquals(6.0, constant.molangSlots[1].eval(ctx), 1e-9);
        assertEquals(6.0, constant.molangSlots[2].eval(ctx), 1e-9);

        BakedBoneChannel keyed = only("\"scale\": {\"0\": 1, \"1\": \"query.anim_time * 2\"}");
        assertNotNull(keyed.molangSlots);
        for (int axis = 0; axis < 3; axis++) {
            assertNull(keyed.molangSlots[axis], "the first key is a plain 1, pre");
            assertNull(keyed.molangSlots[3 + axis], "the first key is a plain 1, post");
            assertEquals(6.0, keyed.molangSlots[6 + axis].eval(ctx), 1e-9, "second key, pre");
            assertEquals(6.0, keyed.molangSlots[9 + axis].eval(ctx), 1e-9, "second key, post");
        }

        BakedBoneChannel prePost = only("\"scale\": {\"0\": 1, \"1\": {\"pre\": \"query.anim_time\", \"post\": 1}}");
        assertNotNull(prePost.molangSlots);
        for (int axis = 0; axis < 3; axis++) {
            assertEquals(3.0, prePost.molangSlots[6 + axis].eval(ctx), 1e-9, "a scalar pre");
            assertNull(prePost.molangSlots[9 + axis], "a plain post");
        }
    }

    @Test
    void aScalarIdentityIsPrunedLikeAnArrayIdentity() {
        assertEquals(0, bake("\"scale\": 1").length);
        assertEquals(0, bake("\"scale\": [1, 1, 1]").length);
        assertEquals(0, bake("\"rotation\": 0").length);
        assertEquals(0, bake("\"scale\": {\"0\": 1, \"1\": 1}").length);
        assertEquals(0, bake("\"scale\": {\"0\": [1, 1, 1], \"1\": [1, 1, 1]}").length);
        assertEquals(0, bake("\"scale\": {\"0\": {\"pre\": 1, \"post\": 1}}").length);

        assertEquals(1, bake("\"scale\": 0").length, "0 is the identity of a rotation, not of a scale");
        assertEquals(1, bake("\"rotation\": 1").length);
        assertEquals(1, bake("\"scale\": {\"0\": 1, \"1\": 0}").length);
    }

    @Test
    void theArrayFormsKeepBakingAsTheyDid() {
        BakedBoneChannel rotation = only("\"rotation\": [90, 0, 90]");
        assertTrue(rotation.constant);
        assertEquals(-Math.PI / 2, rotation.values[0], 1e-6, "X is negated and in radians");
        assertEquals(0f, rotation.values[1], 1e-9f);
        assertEquals(Math.PI / 2, rotation.values[2], 1e-6);

        BakedBoneChannel moving = only("\"position\": {\"0\": [1, 2, 3], \"1\": {\"pre\": [4, 5, 6], \"post\": [7, 8, 9]}}");
        assertArrayEquals(new float[] {0f, 1f}, moving.times);
        assertArrayEquals(new float[] {-1f, 2f, 3f, -1f, 2f, 3f, -4f, 5f, 6f, -7f, 8f, 9f}, moving.values);
    }
}
