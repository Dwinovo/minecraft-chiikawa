package com.dwinovo.chiikawa.anim.pose;

import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.runtime.PoseMixer;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Turns one frame's {@link PoseFrame} into the flat pose buffer a model is drawn from: the
 * whole of "what does the entity look like right now", everything between the controllers'
 * snapshots and the first vertex. Built once per kind of entity, with the providers and
 * interceptors that kind needs, and shared by every entity of it.
 *
 * <p>Composing runs in this order:
 * <ol>
 *   <li>the pose buffer is reset to identity;</li>
 *   <li>the Molang context is reset and filled by the {@link BoneInputProvider}s, in
 *       registration order (the animations read their variables from it as they are
 *       sampled);</li>
 *   <li>every controller's snapshot is sampled into the buffer in registration order, each
 *       per its {@link BlendMode}, so later controllers override or blend on top of earlier
 *       ones;</li>
 *   <li>the {@link BoneInterceptor}s overwrite the bones they own, in
 *       {@link BoneInterceptor.Stage} order and, within a stage, in registration order —
 *       after all controllers, so they cleanly override the affected bones, and so a later
 *       stage sees what an earlier one wrote.</li>
 * </ol>
 *
 * <p>Single-threaded: one Molang context is reused between calls, as the renderers that
 * call this run on one thread.
 */
public final class PoseComposer {

    private final MolangContext molangCtx = new MolangContext();
    /**
     * Pre-sampling MoLang context fillers. Run in registration order before
     * {@code PoseSampler}, so any MoLang expression evaluated during sampling
     * sees the values it needs.
     */
    private final List<BoneInputProvider> inputProviders = new ArrayList<>();
    /**
     * Procedural pose-buffer overrides organised by stage. Stages run in
     * {@link BoneInterceptor.Stage} declaration order; within a stage,
     * interceptors run in registration order.
     */
    private final EnumMap<BoneInterceptor.Stage, List<BoneInterceptor>> interceptors =
            new EnumMap<>(BoneInterceptor.Stage.class);

    /** Register a {@link BoneInputProvider} that fills MoLang context before sampling. */
    public void addInputProvider(BoneInputProvider provider) {
        inputProviders.add(provider);
    }

    /**
     * Register an additional {@link BoneInterceptor} into the named stage.
     * Stages run in {@link BoneInterceptor.Stage} declaration order.
     */
    public void addInterceptor(BoneInterceptor.Stage stage, BoneInterceptor interceptor) {
        interceptors.computeIfAbsent(stage, k -> new ArrayList<>()).add(interceptor);
    }

    /**
     * Composes the pose of {@code model} for one frame.
     *
     * @return a new per-bone (rotation, position, scale) array, {@link PoseSampler#FLOATS_PER_BONE}
     *         floats a bone, indexed by bone index. Allocated per call because a draw may be
     *         deferred; a buffer shared between calls would be overwritten by the next
     *         entity's before the first was drawn.
     */
    public float[] compose(BakedModel model, PoseFrame frame) {
        int boneCount = model.bones.length;
        float[] poseBuf = new float[boneCount * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(poseBuf, boneCount);

        // Frame-level MoLang context. query.anim_time is filled per-channel
        // by PoseSampler; the input providers fill the rest.
        molangCtx.reset();
        for (BoneInputProvider provider : inputProviders) {
            provider.fill(frame, molangCtx);
        }

        long nowNs = frame.animationTimeNs;
        // Iterate controllers in registration order. Each controller writes
        // into the shared pose buffer per its blend mode — later controllers
        // override or blend on top of earlier ones.
        for (ControllerSnapshot snapshot : frame.controllerSnapshots) {
            sampleController(snapshot, nowNs, molangCtx, poseBuf, boneCount);
        }

        // Procedural overrides (head look-at, ear sway, future SpringBone).
        // Run after all controllers so they cleanly override the affected
        // bones; stage order ensures physics-secondary interceptors see the
        // look-at result.
        for (BoneInterceptor.Stage stage : BoneInterceptor.Stage.VALUES) {
            List<BoneInterceptor> stageList = interceptors.get(stage);
            if (stageList == null) continue;
            for (BoneInterceptor interceptor : stageList) {
                interceptor.apply(model, frame, molangCtx, poseBuf);
            }
        }
        return poseBuf;
    }

    /**
     * Sample one controller's contribution into the pose buffer. Three paths:
     *
     * <ol>
     *   <li><b>Crossfade</b> (within-controller switch, e.g. main: idle→sit) —
     *       sample {@code previous} and {@code current} into temp buffers
     *       (each starting from the existing pose buffer so unanimated bones
     *       stay put) and lerp them back into the shared buffer.</li>
     *   <li><b>Stop-fade</b> (controller going silent, e.g. action: harvest
     *       finished) — sample {@code current} into the buffer normally, then
     *       lerp it <em>back toward what was in the buffer before</em> using
     *       the fade alpha. As alpha goes 0→1 the controller's contribution
     *       gradually disappears, blending smoothly to whatever earlier
     *       controllers wrote.</li>
     *   <li><b>Normal</b> — single sample call, fast path.</li>
     * </ol>
     *
     * <p>Both fade paths work for OVERRIDE and ADDITIVE alike — for ADDITIVE
     * the "fully applied" pose is what's in the buffer after sampling, and
     * lerping toward the pre-existing buffer naturally fades the additive
     * delta back to zero.
     */
    private static void sampleController(ControllerSnapshot snapshot, long nowNs,
                                         MolangContext ctx, float[] poseBuf, int boneCount) {
        if (snapshot == null || snapshot.isIdle()) return;

        if (snapshot.hasCrossfade()) {
            crossfade(snapshot, nowNs, ctx, poseBuf, boneCount);
            return;
        }
        if (snapshot.isFadingOut()) {
            stopFade(snapshot, nowNs, ctx, poseBuf, boneCount);
            return;
        }
        PoseSampler.sample(snapshot.current(), snapshot.blendMode(), nowNs, ctx, poseBuf);
    }

    /**
     * Within-controller crossfade. Both channels are sampled on top of the
     * existing pose buffer (so bones neither animation touches keep their
     * earlier-controller values), then lerped per
     * {@link PoseMixer#fadeAlpha smoothstep alpha}.
     */
    private static void crossfade(ControllerSnapshot snapshot, long nowNs,
                                  MolangContext ctx, float[] poseBuf, int boneCount) {
        int len = boneCount * PoseSampler.FLOATS_PER_BONE;
        float[] fromPose = new float[len];
        float[] toPose   = new float[len];
        System.arraycopy(poseBuf, 0, fromPose, 0, len);
        System.arraycopy(poseBuf, 0, toPose,   0, len);

        BlendMode mode = snapshot.blendMode();
        PoseSampler.sample(snapshot.previous(), mode, nowNs, ctx, fromPose);
        PoseSampler.sample(snapshot.current(),  mode, nowNs, ctx, toPose);
        float alpha = PoseMixer.fadeAlpha(snapshot.fadeStartNs(), snapshot.fadeDurationSec(), nowNs);
        PoseMixer.blend(fromPose, toPose, alpha, poseBuf, boneCount);
    }

    /**
     * Stop-fade: the controller is going silent, but its contribution lerps
     * toward the buffer's pre-sample state (= whatever earlier controllers
     * wrote) over {@code fadeDurationSec}.
     *
     * <p>Implementation: snapshot the buffer, apply the controller's full
     * contribution, then lerp from "with contribution" toward "without"
     * as alpha goes 0→1. At alpha=0 the controller still fully contributes;
     * at alpha=1 it's effectively absent.
     */
    private static void stopFade(ControllerSnapshot snapshot, long nowNs,
                                 MolangContext ctx, float[] poseBuf, int boneCount) {
        int len = boneCount * PoseSampler.FLOATS_PER_BONE;
        float[] existing = new float[len];
        System.arraycopy(poseBuf, 0, existing, 0, len);

        PoseSampler.sample(snapshot.current(), snapshot.blendMode(), nowNs, ctx, poseBuf);

        float alpha = PoseMixer.fadeAlpha(snapshot.fadeStartNs(), snapshot.fadeDurationSec(), nowNs);
        // lerp(poseBuf, existing, alpha) → poseBuf
        // alpha=0: full contribution remains; alpha=1: fully reverts to existing.
        PoseMixer.blend(poseBuf, existing, alpha, poseBuf, boneCount);
    }
}
