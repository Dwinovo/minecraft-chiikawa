package com.dwinovo.chiikawa.anim.render.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Which way a held item points once it is in the fist. The model's frame has up up and the
 * pet facing {@code -Z}; vanilla's third-person display transforms expect the item's up to
 * run out of the front of the fist and its back to face the sky.
 */
class HeldItemLayerTest {
    private static final float EPSILON = 1.0E-5F;

    @Test
    void anItemsUpPointsTheWayThePetFaces() {
        assertDirection(new Vector3f(0, 0, -1), direction(new Vector3f(0, 1, 0)),
            "the item's up does not point ahead of the fist, so a sword stands on its pommel");
    }

    @Test
    void anItemsFaceLooksUp() {
        assertDirection(new Vector3f(0, 1, 0), direction(new Vector3f(0, 0, 1)),
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

    private static Vector3f direction(Vector3f itemAxis) {
        PoseStack pose = new PoseStack();
        HeldItemLayer.intoFist(pose);
        return pose.last().pose().transformDirection(itemAxis);
    }

    private static void assertDirection(Vector3f expected, Vector3f actual, String message) {
        assertEquals(expected.x, actual.x, EPSILON, message + ": " + actual);
        assertEquals(expected.y, actual.y, EPSILON, message + ": " + actual);
        assertEquals(expected.z, actual.z, EPSILON, message + ": " + actual);
    }
}
