package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import net.minecraft.util.Mth;

/**
 * {@link BoneInterceptor.Stage#PHYSICS_SECONDARY} interceptor that drives the
 * canned idle motion shared by every pet:
 *
 * <ul>
 *   <li>{@code LeftEar} / {@code RightEar} — symmetric idle sway driven by
 *       {@code ageInTicks}, plus a backwards lean proportional to
 *       {@code walkSpeed} for a runner-leans-back effect</li>
 *   <li>{@code tail} — gentle Y-axis wag</li>
 * </ul>
 *
 * <p>This is hand-tuned procedural motion (cos/sin of age), not a real
 * spring-bone solver. The placeholder will be superseded once a proper
 * SpringBone interceptor lands, but the stage assignment stays the same so
 * the replacement can be dropped in without disturbing the rest of the
 * pipeline.
 */
public final class IdleSwayInterceptor implements BoneInterceptor {

    private static final float BREATHING_SPEED   = 0.1F;
    private static final float EAR_SWING_AMOUNT  = 0.1F;
    private static final float EAR_TWIST_AMOUNT  = 0.1F;
    private static final float TAIL_WAG_AMOUNT   = 0.15F;

    @Override
    public void apply(BakedModel model, ChiikawaRenderState state, MolangContext ctx, float[] poseBuf) {
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
