package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedCube;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

/**
 * Stateless mesh emitter. Walks a {@link BakedModel}'s bone DAG with a
 * {@link PoseStack} and pushes 6 face quads per cube into the supplied
 * vertex consumer.
 *
 * <p>Phase 1 only applies the bone's rest rotation. Animation pose blending
 * arrives in Phase 2 — the {@code render} signature will gain a per-bone
 * pose buffer parameter then.
 */
public final class ModelRenderer {

    /**
     * Per-face vertex order: TL, BL, BR, TR (CCW from outside the cube).
     * Indices reference the 8 cube corners encoded as a 3-bit mask:
     * bit0=X(max if 1), bit1=Y(max if 1), bit2=Z(max if 1).
     */
    private static final int[][] FACE_CORNERS = {
            // NORTH (-Z): viewed from -Z; +X visually on left.
            {0b011, 0b001, 0b000, 0b010},
            // SOUTH (+Z): viewed from +Z; +X on right.
            {0b110, 0b100, 0b101, 0b111},
            // WEST (-X)
            {0b010, 0b000, 0b100, 0b110},
            // EAST (+X)
            {0b111, 0b101, 0b001, 0b011},
            // UP (+Y): TL = NW corner, +X right, +Z down.
            {0b010, 0b110, 0b111, 0b011},
            // DOWN (-Y): TL = SW corner.
            {0b100, 0b000, 0b001, 0b101},
    };

    private static final float[][] FACE_NORMALS = {
            {0, 0, -1},
            {0, 0, 1},
            {-1, 0, 0},
            {1, 0, 0},
            {0, 1, 0},
            {0, -1, 0},
    };

    private final Quaternionf rotBuf = new Quaternionf();

    /**
     * Renders the entire model. The caller's {@code initial} pose should already
     * include the entity transform, body rotation, and pixel→block scale.
     */
    public void render(BakedModel model, PoseStack.Pose initial, VertexConsumer vc,
                       int packedLight, int packedOverlay) {
        PoseStack stack = new PoseStack();
        stack.last().set(initial);
        for (int rootIdx : model.rootBones) {
            renderBone(model, rootIdx, stack, vc, packedLight, packedOverlay);
        }
    }

    private void renderBone(BakedModel model, int boneIdx, PoseStack stack,
                            VertexConsumer vc, int packedLight, int packedOverlay) {
        BakedBone bone = model.bones[boneIdx];
        stack.pushPose();
        if (bone.hasRestRotation) {
            rotBuf.identity().rotationXYZ(bone.restRotX, bone.restRotY, bone.restRotZ);
            stack.last().rotateAround(rotBuf, bone.pivotX, bone.pivotY, bone.pivotZ);
        }

        for (int c = bone.cubeStart, end = bone.cubeStart + bone.cubeCount; c < end; c++) {
            renderCube(model.cubes[c], stack, vc, packedLight, packedOverlay);
        }
        for (int childIdx : bone.children) {
            renderBone(model, childIdx, stack, vc, packedLight, packedOverlay);
        }

        stack.popPose();
    }

    private void renderCube(BakedCube cube, PoseStack stack, VertexConsumer vc,
                            int packedLight, int packedOverlay) {
        stack.pushPose();
        if (cube.hasRotation) {
            rotBuf.identity().rotationXYZ(cube.rotX, cube.rotY, cube.rotZ);
            stack.last().rotateAround(rotBuf, cube.pivotX, cube.pivotY, cube.pivotZ);
        }

        PoseStack.Pose pose = stack.last();
        float invW = 1.0f / cube.texW;
        float invH = 1.0f / cube.texH;

        for (int face = 0; face < 6; face++) {
            int[] corners = FACE_CORNERS[face];
            float[] n = FACE_NORMALS[face];
            for (int v = 0; v < 4; v++) {
                int mask = corners[v];
                float x = ((mask & 1) != 0) ? cube.maxX : cube.minX;
                float y = ((mask & 2) != 0) ? cube.maxY : cube.minY;
                float z = ((mask & 4) != 0) ? cube.maxZ : cube.minZ;
                float u = cube.faceUV[face][v][0] * invW;
                float vv = cube.faceUV[face][v][1] * invH;
                vc.addVertex(pose, x, y, z)
                        .setColor(255, 255, 255, 255)
                        .setUv(u, vv)
                        .setOverlay(packedOverlay)
                        .setLight(packedLight)
                        .setNormal(pose, n[0], n[1], n[2]);
            }
        }

        stack.popPose();
    }
}
