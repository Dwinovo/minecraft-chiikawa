package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.baked.BakedBone;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * {@link BoneInterceptor.Stage#LOOK_AT} interceptor that points {@code AllHead}
 * along the entity's view direction.
 *
 * <p>Reads {@link ChiikawaRenderState#netHeadYaw} and {@link ChiikawaRenderState#headPitch}
 * — both captured at extract time — rather than the live {@code yRot} /
 * {@code bodyRot} fields, which {@code InventoryScreen} overwrites with
 * mouse-derived values during inventory previews.
 *
 * <p>The look is taken in the pet's own frame, not the head's parent's: the pets have no
 * neck, so the head hangs straight off a body that animations lean (sitting tips the upper
 * body forward and the head back). Turned about a leaning body's axis, a head looking aside
 * rolls over; so the look is carried into the parent's frame (P⁻¹·look·P) and the head's
 * own animated turn goes on after it. Whatever the body does, the pet turns its head about
 * the upright and keeps the pose the animation gave it.
 */
public final class HeadLookInterceptor implements BoneInterceptor {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final Quaternionf parent = new Quaternionf();
    private final Quaternionf turn = new Quaternionf();
    private final Quaternionf look = new Quaternionf();
    private final Quaternionf local = new Quaternionf();
    private final Vector3f euler = new Vector3f();

    @Override
    public void apply(BakedModel model, ChiikawaRenderState state, MolangContext ctx, float[] poseBuf) {
        Integer headIdx = model.boneIndex.get("AllHead");
        if (headIdx == null) return;
        int base = headIdx * PoseSampler.FLOATS_PER_BONE;

        // How the bones above the head are turned this frame, root first.
        parent.identity();
        for (int i = model.bones[headIdx].parentIdx; i >= 0; i = model.bones[i].parentIdx) {
            BakedBone bone = model.bones[i];
            int at = i * PoseSampler.FLOATS_PER_BONE;
            BedrockRotation.of(turn, bone.restRotX + poseBuf[at], bone.restRotY + poseBuf[at + 1],
                bone.restRotZ + poseBuf[at + 2]);
            parent.premul(turn);
        }

        BedrockRotation.of(look, -state.headPitch * DEG_TO_RAD, -state.netHeadYaw * DEG_TO_RAD, 0f);
        BedrockRotation.of(turn, poseBuf[base], poseBuf[base + 1], poseBuf[base + 2]);
        // P⁻¹ · look · P · (the head's own animated turn)
        local.set(parent).conjugate().mul(look).mul(parent).mul(turn);
        BedrockRotation.angles(local, euler);
        poseBuf[base]     = euler.x;
        poseBuf[base + 1] = euler.y;
        poseBuf[base + 2] = euler.z;
    }
}
