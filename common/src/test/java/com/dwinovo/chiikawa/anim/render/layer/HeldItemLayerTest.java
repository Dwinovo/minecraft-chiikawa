package com.dwinovo.chiikawa.anim.render.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.baked.LoopMode;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import com.dwinovo.chiikawa.anim.render.ChiikawaRenderState;
import com.dwinovo.chiikawa.anim.render.PetData;
import com.dwinovo.chiikawa.anim.render.PropBone;
import com.dwinovo.chiikawa.camera.CameraCarry;
import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
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
        HeldItemLayer layer = new HeldItemLayer(List.of(PropBone.EXAM_PENCIL));

        assertTrue(layer.handsFull(frame("exam", false)), "a pet writing its exam still holds its sword");
        assertFalse(layer.handsFull(frame("idle", false)), "a pet holds nothing while no prop is out");
        assertFalse(layer.handsFull(frame("exam", true)), "a pet holds nothing while its pencil is put away");
    }

    @Test
    void aPetWithoutThePropKeepsItsItem() {
        HeldItemLayer layer = new HeldItemLayer(List.of(PropBone.GUITAR));

        assertFalse(layer.handsFull(frame("guitar", false)), "a pet put its item away for a prop it does not have");
    }

    @Test
    void aCameraHangingAtTheChestLeavesTheHandsFree() {
        HeldItemLayer layer = new HeldItemLayer(List.of(PropBone.CAMERA));

        assertFalse(layer.handsFull(frame("idle", false, "camera")), "a camera on the chest took the sword out of the fist");
        assertTrue(layer.handsFull(frame("photo", false, "camera")), "a camera held up to the face is in the hands");
    }

    @Test
    void aCameraInTheMainHandIsNotDrawnInTheFist() {
        ChiikawaRenderState state = new ChiikawaRenderState();

        state.put(PetData.CAMERA_CARRY, CameraCarry.HAND);
        assertTrue(HeldItemLayer.cameraInHand(state), "a camera in the hand is drawn in the fist as well as on the chest");
        state.put(PetData.CAMERA_CARRY, CameraCarry.POCKET);
        assertFalse(HeldItemLayer.cameraInHand(state), "a sword was put away for a camera in a pocket");
        state.put(PetData.CAMERA_CARRY, CameraCarry.NONE);
        assertFalse(HeldItemLayer.cameraInHand(state));
        assertFalse(HeldItemLayer.cameraInHand(new ChiikawaRenderState()), "an unset state keeps the item");
    }

    /**
     * A frame of a model with a hand, a pencil and a camera, the pencil drawn or not, while the
     * pet plays an animation.
     */
    private static RenderLayerContext frame(String animation, boolean pencilHidden) {
        return frame(animation, pencilHidden, "ExamPencil");
    }

    /** As above, with the bone named {@code prop} in place of the pencil, drawn or not. */
    private static RenderLayerContext frame(String animation, boolean hidden, String prop) {
        BakedBone hand = new BakedBone("RightHandLocator", -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0]);
        BakedBone other = new BakedBone(prop, -1, 0, 0, 0, 0, 0, 0, false, 0, 0, new int[0]);
        BakedModel model = new BakedModel(new BakedBone[] {hand, other}, new BakedCube[0], new int[] {0, 1},
            Map.of("RightHandLocator", 0, prop, 1), 64, 64);
        ChiikawaRenderState state = new ChiikawaRenderState();
        state.hiddenBones = new boolean[] {false, hidden};
        BakedAnimation baked = new BakedAnimation(animation, 1f, LoopMode.LOOP, new BakedBoneChannel[0]);
        state.controllerSnapshots = new ControllerSnapshot[] {new ControllerSnapshot("main", BlendMode.OVERRIDE,
            new AnimationChannel(baked, 0L, true), null, 0L, 0f, false)};
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
