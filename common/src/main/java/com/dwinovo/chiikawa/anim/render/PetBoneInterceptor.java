package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import net.minecraft.util.Mth;

/**
 * Replicates the legacy {@code AbstractPetRender.adjustModelBonesForRender}
 * behaviour on the new pipeline:
 *
 * <ul>
 *   <li>{@code AllHead} — yaw / pitch follow the player's view (clamped by
 *       vanilla head-rot logic upstream)</li>
 *   <li>{@code LeftEar} / {@code RightEar} — symmetric idle sway driven by
 *       {@code ageInTicks}, plus a backwards lean proportional to
 *       {@code walkSpeed} for a runner-leans-back effect</li>
 *   <li>{@code tail} — gentle Y-axis wag</li>
 * </ul>
 *
 * The previous renderer wrote additive rotations that compose with the
 * bone's rest pose exactly the same way our pose buffer does. Values port
 * over 1:1 — no extra X-mirror needed because
 * both pipelines apply the same bake-time mirror to the rest pose, and the
 * procedural deltas are already authored in that mirrored frame.
 */
public final class PetBoneInterceptor implements BoneInterceptor {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float BREATHING_SPEED   = 0.1F;
    private static final float EAR_SWING_AMOUNT  = 0.1F;
    private static final float EAR_TWIST_AMOUNT  = 0.1F;
    private static final float TAIL_WAG_AMOUNT   = 0.15F;

    @Override
    public void apply(BakedModel model, ChiikawaRenderState state, MolangContext ctx, float[] poseBuf) {
        Integer headIdx = model.boneIndex.get("AllHead");
        if (headIdx != null) {
            // netHeadYaw / headPitch are captured at extract time; reading them
            // back from yRot / bodyRot here would break in inventory previews
            // (InventoryScreen overwrites those fields with mouse-derived values).
            int base = headIdx * PoseSampler.FLOATS_PER_BONE;
            poseBuf[base]     = -state.headPitch  * DEG_TO_RAD;  // X (pitch)
            poseBuf[base + 1] = -state.netHeadYaw * DEG_TO_RAD;  // Y (yaw)
            poseBuf[base + 2] = 0f;                              // Z
        }

        float age = state.ageInTicks;
        float limb = state.walkSpeed;
        float swayCos = Mth.cos(age * BREATHING_SPEED);
        float swaySin = Mth.sin(age * BREATHING_SPEED);
        float earBackwardSwing = -limb * 1.0F;

        Integer leftEar = model.boneIndex.get("LeftEar");
        if (leftEar != null) {
            int base = leftEar * PoseSampler.FLOATS_PER_BONE;
            poseBuf[base]     = 0f;
            poseBuf[base + 1] = swayCos * EAR_SWING_AMOUNT - earBackwardSwing;
            poseBuf[base + 2] = swaySin * EAR_TWIST_AMOUNT;
        }

        Integer rightEar = model.boneIndex.get("RightEar");
        if (rightEar != null) {
            int base = rightEar * PoseSampler.FLOATS_PER_BONE;
            poseBuf[base]     = 0f;
            poseBuf[base + 1] = -swayCos * EAR_SWING_AMOUNT + earBackwardSwing;
            poseBuf[base + 2] = -swaySin * EAR_TWIST_AMOUNT;
        }

        Integer tail = model.boneIndex.get("tail");
        if (tail != null) {
            int base = tail * PoseSampler.FLOATS_PER_BONE;
            poseBuf[base]     = 0f;
            poseBuf[base + 1] = swayCos * TAIL_WAG_AMOUNT;
            poseBuf[base + 2] = 0f;
        }
    }
}
