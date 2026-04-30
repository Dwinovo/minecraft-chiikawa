package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * Submits an {@link ItemStack} at a named bone's pivot.
 *
 * <h2>Chain transform</h2>
 * Walks the bone chain root → target reproducing the same
 * {@code T(pivot + dPos) · R(rest + delta) · S · T(-pivot)} composition that
 * {@link ModelRenderer} applies. For the chain's last bone the trailing
 * {@code T(-pivot)} is omitted so the {@link PoseStack} ends up at the
 * locator's pivot (with parent rotations applied), which is the natural
 * anchor for item display transforms.
 *
 * <h2>Unit conversion</h2>
 * The caller's {@code PoseStack} is in 1/16-scaled pixel space — bone pivots
 * use raw pixel values so {@code translate(pivotX)} works directly. Items
 * however render in <b>block</b> units (their {@code BakedQuad} verts are
 * 0..1, display transforms specify block-sized translations). Without a
 * compensating {@code scale(16)} the rendered item ends up at 1/16 of its
 * intended size. Vanilla {@code LivingEntityRenderer} dodges this by keeping
 * the {@code PoseStack} in block units and pushing the 1/16 factor into
 * {@code ModelPart} vertex emission instead — we apply the inverse only
 * here, isolated to the item attachment.
 */
public final class BoneAttachmentLayer {

    private final Quaternionf rotBuf = new Quaternionf();
    /** Reusable buffer for the chain root → target. */
    private int[] chainBuf = new int[8];

    /**
     * Walks the chain root → {@code targetBoneName} and renders {@code stack}
     * at the resulting pivot. No-ops when the stack is empty or the bone is
     * absent from the model.
     */
    public void render(BakedModel model, float[] poseBuf, String targetBoneName,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       ItemStack stack, int packedLight) {
        if (stack.isEmpty()) return;
        Integer targetIdx = model.boneIndex.get(targetBoneName);
        if (targetIdx == null) return;

        // Resolve the item model fresh per render. The state is small and
        // short-lived, so the allocation is cheaper than the bookkeeping of a
        // cached instance.
        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState itemRenderState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(
                itemRenderState,
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false,
                mc.level,
                null,
                0);
        if (itemRenderState.isEmpty()) return;

        int chainLen = buildChain(model, targetIdx);

        poseStack.pushPose();
        for (int i = 0; i < chainLen; i++) {
            applyBoneTransform(model, poseBuf, chainBuf[i], poseStack, i == chainLen - 1);
        }
        // Cancel the entity-level scale(1/16): items expect block-unit space.
        poseStack.scale(16f, 16f, 16f);
        itemRenderState.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** Fills {@link #chainBuf} with bone indices root → target. Returns the length. */
    private int buildChain(BakedModel model, int targetIdx) {
        int len = 0;
        int idx = targetIdx;
        while (idx >= 0) {
            len++;
            idx = model.bones[idx].parentIdx;
        }
        if (chainBuf.length < len) chainBuf = new int[Math.max(len, chainBuf.length * 2)];
        idx = targetIdx;
        for (int i = len - 1; i >= 0; i--) {
            chainBuf[i] = idx;
            idx = model.bones[idx].parentIdx;
        }
        return len;
    }

    private void applyBoneTransform(BakedModel model, float[] poseBuf, int boneIdx,
                                    PoseStack poseStack, boolean isTarget) {
        BakedBone bone = model.bones[boneIdx];
        int base = boneIdx * PoseSampler.FLOATS_PER_BONE;
        float dRotX = poseBuf[base];
        float dRotY = poseBuf[base + 1];
        float dRotZ = poseBuf[base + 2];
        float dPosX = poseBuf[base + 3];
        float dPosY = poseBuf[base + 4];
        float dPosZ = poseBuf[base + 5];
        float sX    = poseBuf[base + 6];
        float sY    = poseBuf[base + 7];
        float sZ    = poseBuf[base + 8];

        float rotX = bone.restRotX + dRotX;
        float rotY = bone.restRotY + dRotY;
        float rotZ = bone.restRotZ + dRotZ;
        boolean hasRot   = rotX != 0f || rotY != 0f || rotZ != 0f;
        boolean hasScale = sX != 1f || sY != 1f || sZ != 1f;
        boolean hasPos   = dPosX != 0f || dPosY != 0f || dPosZ != 0f;

        if (isTarget) {
            // Target bone is the item anchor — always translate to (pivot+dPos)
            // so the PoseStack ends at the locator's pivot, with rotation/scale
            // composed on top. The trailing T(-pivot) is intentionally omitted
            // so the item attaches at the pivot rather than absolute zero.
            poseStack.translate(bone.pivotX + dPosX, bone.pivotY + dPosY, bone.pivotZ + dPosZ);
            if (hasRot) {
                rotBuf.identity().rotationXYZ(rotX, rotY, rotZ);
                poseStack.mulPose(rotBuf);
            }
            if (hasScale) {
                poseStack.scale(sX, sY, sZ);
            }
            return;
        }

        // Non-target chain links: same identity / pos-only fast paths as
        // ModelRenderer.renderBone — collapse the pivot sandwich when no
        // rotation/scale is in play, no-op when nothing is in play.
        if (hasRot || hasScale) {
            poseStack.translate(bone.pivotX + dPosX, bone.pivotY + dPosY, bone.pivotZ + dPosZ);
            if (hasRot) {
                rotBuf.identity().rotationXYZ(rotX, rotY, rotZ);
                poseStack.mulPose(rotBuf);
            }
            if (hasScale) {
                poseStack.scale(sX, sY, sZ);
            }
            poseStack.translate(-bone.pivotX, -bone.pivotY, -bone.pivotZ);
        } else if (hasPos) {
            poseStack.translate(dPosX, dPosY, dPosZ);
        }
    }
}
