package com.dwinovo.chiikawa.anim.render.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.render.ChiikawaRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Which way a held item points once it is in the fist. The model's frame has up up and the
 * pet facing {@code -Z}; vanilla's third-person display transforms expect the item's up to
 * run out of the front of the fist and its back to face the sky — lifted a tenth of a turn,
 * as vanilla lifts an arm that holds something. And that it is put away while the pet's
 * hands are full with something of its own model.
 */
class HeldItemLayerTest {
    private static final float EPSILON = 1.0E-5F;
    private static final float LIFT = (float) (Math.PI / 10.0);

    @Test
    void anItemsUpPointsAheadAndALittleUp() {
        assertDirection(new Vector3f(0, (float) Math.sin(LIFT), (float) -Math.cos(LIFT)),
            direction(new Vector3f(0, 1, 0)),
            "the item's up does not point ahead of the fist, so a tool stands on end or digs into the ground");
    }

    @Test
    void anItemsFaceLooksUp() {
        assertDirection(new Vector3f(0, (float) Math.cos(LIFT), (float) Math.sin(LIFT)),
            direction(new Vector3f(0, 0, 1)),
            "the item's face is not turned to the sky");
    }

    @Test
    void anItemsSideStaysToTheSide() {
        assertDirection(new Vector3f(1, 0, 0), direction(new Vector3f(1, 0, 0)),
            "the item was turned over sideways");
    }

    @Test
    void theGripSitsAheadOfTheKnucklesAndIntoThePalm() {
        PoseStack pose = new PoseStack();
        HeldItemLayer.intoFist(pose);
        Vector3f grip = pose.last().pose().transformPosition(new Vector3f());

        assertDirection(new Vector3f(0, -0.0625F, -0.125F), grip, "the item is not a pixel down and two ahead");
    }

    @Test
    void aPetHoldsThingsSmallerThanAPlayerDoes() {
        PoseStack pose = new PoseStack();
        HeldItemLayer.intoFist(pose);
        float size = pose.last().pose().transformDirection(new Vector3f(1, 0, 0)).length();

        assertEquals(true, size > 0.0F && size < 1.0F, "a held item is drawn at a player's size: " + size);
    }

    @Test
    void anItemIsPutAwayWhileAPropIsInTheHands() {
        HeldItemLayer layer = new HeldItemLayer(List.of("ExamPencil"));

        assertTrue(layer.handsFull(frame(false)), "a pet writing its exam still holds its sword");
        assertFalse(layer.handsFull(frame(true)), "a pet holds nothing while its pencil is put away");
    }

    @Test
    void aPetWithoutThePropKeepsItsItem() {
        HeldItemLayer layer = new HeldItemLayer(List.of("guitar"));

        assertFalse(layer.handsFull(frame(false)), "a pet put its item away for a prop it does not have");
    }

    /** A frame of a model with a hand and a pencil, the pencil drawn or not. */
    private static RenderLayerContext frame(boolean pencilHidden) {
        BakedBone hand = new BakedBone("RightHandLocator", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0]);
        BakedBone pencil = new BakedBone("ExamPencil", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0]);
        BakedModel model = new BakedModel(new BakedBone[] {hand, pencil}, new BakedCube[0], new int[] {0, 1},
            Map.of("RightHandLocator", 0, "ExamPencil", 1), 64, 64);
        ChiikawaRenderState state = new ChiikawaRenderState();
        state.hiddenBones = new boolean[] {false, pencilHidden};
        return new RenderLayerContext(model, new float[0], state, new PoseStack(), null, 0, 0);
    }

    /** Which way an item's axis ends up, whatever size the hand makes it. */
    private static Vector3f direction(Vector3f itemAxis) {
        PoseStack pose = new PoseStack();
        HeldItemLayer.intoFist(pose);
        return pose.last().pose().transformDirection(itemAxis).normalize();
    }

    private static void assertDirection(Vector3f expected, Vector3f actual, String message) {
        assertEquals(expected.x, actual.x, EPSILON, message + ": " + actual);
        assertEquals(expected.y, actual.y, EPSILON, message + ": " + actual);
        assertEquals(expected.z, actual.z, EPSILON, message + ": " + actual);
    }
}
