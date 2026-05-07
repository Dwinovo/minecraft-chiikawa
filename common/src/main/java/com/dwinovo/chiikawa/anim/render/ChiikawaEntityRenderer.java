package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.AnimationLibrary;
import com.dwinovo.chiikawa.anim.api.ChiikawaAnimated;
import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.render.layer.HeldItemLayer;
import com.dwinovo.chiikawa.anim.render.layer.RenderLayer;
import com.dwinovo.chiikawa.anim.render.layer.RenderLayerContext;
import com.dwinovo.chiikawa.anim.runtime.PetAnimator;
import com.dwinovo.chiikawa.anim.runtime.PoseMixer;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.dwinovo.chiikawa.anim.runtime.SlotState;
import com.dwinovo.chiikawa.anim.state.PetAnimPlan;
import com.dwinovo.chiikawa.anim.state.PetAnimationResolver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Base entity renderer for Bedrock-format pets. Handles model lookup, the
 * Bedrock entity transform (Y rotation + pixel scale), and per-frame pose
 * sampling.
 *
 * <p>{@code modelKey} is the {@code namespace:path} address under which the
 * baked model was registered (e.g. {@code chiikawa:chiikawa}); the matching
 * texture is resolved at render time from the {@code textureLocation} field,
 * and the default loop animation is registered as
 * {@code <modelKey>/<DEFAULT_LOOP_NAME>} (e.g. {@code chiikawa:chiikawa/idle}).
 */
public abstract class ChiikawaEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    private static final float PIXEL_SCALE = 1.0f / 16.0f;
    /** Animation name played on the main channel when nothing else is set. */
    private static final String DEFAULT_LOOP_NAME = "idle";

    protected final ResourceLocation modelKey;
    protected final ResourceLocation textureLocation;
    protected final ResourceLocation defaultLoopKey;

    private final ModelRenderer mesh = new ModelRenderer();
    private final Quaternionf rotBuf = new Quaternionf();
    private final MolangContext molangCtx = new MolangContext();
    /**
     * Procedural pose-buffer overrides organised by stage. Stages run in
     * {@link BoneInterceptor.Stage} declaration order; within a stage,
     * interceptors run in registration order. Subclasses register additional
     * interceptors from their constructor via {@link #addInterceptor}.
     */
    private final EnumMap<BoneInterceptor.Stage, List<BoneInterceptor>> interceptors =
            new EnumMap<>(BoneInterceptor.Stage.class);
    /**
     * Visual layers that run after the main mesh submits, in registration order.
     * Subclasses register additional layers (props, emissive overlays, capes...)
     * via {@link #addRenderLayer(RenderLayer)} from their constructor.
     */
    private final List<RenderLayer> renderLayers = new ArrayList<>();

    protected ChiikawaEntityRenderer(EntityRendererProvider.Context ctx, String name) {
        super(ctx);
        this.modelKey = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name);
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                "textures/entities/" + name + ".png");
        this.defaultLoopKey = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                name + "/" + DEFAULT_LOOP_NAME);
        // Default interceptors shared by all pets. Subclasses can append more.
        addInterceptor(BoneInterceptor.Stage.LOOK_AT, new HeadLookInterceptor());
        addInterceptor(BoneInterceptor.Stage.PHYSICS_SECONDARY, new IdleSwayInterceptor());
        // Default layers shared by all pets.
        addRenderLayer(new HeldItemLayer());
    }

    /**
     * Register an additional {@link BoneInterceptor} into the named stage.
     * Stages run in {@link BoneInterceptor.Stage} declaration order.
     */
    protected final void addInterceptor(BoneInterceptor.Stage stage, BoneInterceptor interceptor) {
        interceptors.computeIfAbsent(stage, k -> new ArrayList<>()).add(interceptor);
    }

    /** Register an additional {@link RenderLayer}; runs after the main mesh in registration order. */
    protected final void addRenderLayer(RenderLayer layer) {
        renderLayers.add(layer);
    }

    private void fillRenderState(T entity, ChiikawaRenderState state, float partialTick) {
        state.modelKey = modelKey;
        state.texture = textureLocation;
        state.mainSlot = SlotState.EMPTY;
        state.subSlots = null;
        state.walkSpeed = 0.0f;
        state.bodyRot = 0.0f;
        state.ageInTicks = entity.tickCount + partialTick;
        state.netHeadYaw = 0.0f;
        state.headPitch = 0.0f;

        if (entity instanceof LivingEntity living) {
            float bodyRot = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            float headRot = Mth.rotLerp(partialTick, living.yHeadRotO, living.getYHeadRot());
            float pitch   = Mth.rotLerp(partialTick, living.xRotO, living.getXRot());
            state.bodyRot = bodyRot;
            state.ageInTicks = living.tickCount + partialTick;
            state.walkSpeed = living.walkAnimation.speed(partialTick);
            state.netHeadYaw = Mth.wrapDegrees(headRot - bodyRot);
            state.headPitch  = pitch;

            // Stash the live mainhand stack. HeldItemLayer resolves it into
            // a fresh ItemStackRenderState while rendering. Lives on the typed
            // extras map so future layers can register their own keys without
            // adding fields to ChiikawaRenderState.
            state.put(PetData.HELD_ITEM_STACK, living.getMainHandItem());
        }

        if (entity instanceof ChiikawaAnimated animated) {
            PetAnimator animator = animated.getPetAnimator();
            animator.clearFinished(System.nanoTime());
            // Pick the desired main loop based on entity state. setMain is
            // idempotent — switches only when the wanted animation actually
            // changes, so a second extractRenderState in the same frame
            // (InventoryScreen.renderEntityInInventoryFollowsMouse) does not
            // restart the timer.
            PetAnimPlan plan = PetAnimationResolver.resolve(animated.getAnimContext(state.walkSpeed));
            BakedAnimation wanted = firstAvailable(plan.baseLoopCandidates());
            if (wanted == null) wanted = AnimationLibrary.get(defaultLoopKey);
            if (wanted != null) {
                animator.setMain(wanted, true);
            }
            state.mainSlot = animator.get(PetAnimator.Slot.BASE);
            // Snapshot any non-BASE slots populated by trigger(). SlotState
            // records are immutable so this is a safe shallow copy.
            SlotState[] subs = null;
            PetAnimator.Slot[] slotKeys = PetAnimator.Slot.VALUES;
            for (int i = 1; i < slotKeys.length; i++) {
                SlotState sub = animator.get(slotKeys[i]);
                if (sub.isEmpty()) continue;
                if (subs == null) subs = new SlotState[slotKeys.length - 1];
                subs[i - 1] = sub;
            }
            state.subSlots = subs;
        }
    }

    private ResourceLocation animKey(String name) {
        return ResourceLocation.fromNamespaceAndPath(modelKey.getNamespace(),
                modelKey.getPath() + "/" + name);
    }

    private BakedAnimation firstAvailable(Iterable<String> names) {
        for (String name : names) {
            BakedAnimation animation = AnimationLibrary.get(animKey(name));
            if (animation != null) {
                return animation;
            }
        }
        return null;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return textureLocation;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        ChiikawaRenderState state = new ChiikawaRenderState();
        fillRenderState(entity, state, partialTick);
        renderState(entity, state, poseStack, bufferSource, packedLight);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderState(T entity, ChiikawaRenderState state, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight) {
        BakedModel model = ModelLibrary.get(state.modelKey);
        if (model == null) {
            return;
        }

        int boneCount = model.bones.length;
        float[] poseBuf = new float[boneCount * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(poseBuf, boneCount);

        // Frame-level Molang context. query.anim_time is filled per-channel
        // by PoseSampler; ground_speed feeds the locomotion math. Head /
        // ear / tail orientation is owned by PetBoneInterceptor — see
        // MolangContext for why ysm.* and v.L*_P* are deliberately not in
        // scope.
        molangCtx.reset();
        molangCtx.vars[MolangContext.SLOT_GROUND_SPEED] = state.walkSpeed;

        long nowNs = System.nanoTime();
        sampleSlot(state.mainSlot, nowNs, molangCtx, poseBuf, boneCount);
        if (state.subSlots != null) {
            // Sub-slot fades aren't supported yet — the cross-blend would
            // overwrite bones the slot doesn't actually animate, undoing the
            // BASE pose for unrelated bones. Sample current channel only.
            for (SlotState sub : state.subSlots) {
                if (sub == null || sub.isEmpty()) continue;
                PoseSampler.sample(sub.current(), nowNs, molangCtx, poseBuf);
            }
        }
        // Procedural overrides (head look-at, ear sway, tail wag, future
        // SpringBone). Run after sampling so they cleanly replace the
        // animation's contribution to the affected bones; stage order ensures
        // physics-secondary interceptors see the look-at result.
        for (BoneInterceptor.Stage stage : BoneInterceptor.Stage.VALUES) {
            List<BoneInterceptor> stageList = interceptors.get(stage);
            if (stageList == null) continue;
            for (BoneInterceptor interceptor : stageList) {
                interceptor.apply(model, state, molangCtx, poseBuf);
            }
        }

        poseStack.pushPose();
        // Bedrock entity rendering, aligned to the Blockbench display.
        //  - Model forward = -Z (Bedrock convention). bodyRot=0 = entity faces +Z
        //    (south), so rotateY(180 - bodyRot) aligns the model with world facing.
        //  - The X mirror needed to undo Blockbench's display→JSON X negation is
        //    applied at BAKE TIME inside ModelBaker (and AnimationBaker for animation
        //    deltas). The renderer therefore uses a clean positive pixel scale here.
        //  - Y is not flipped (Bedrock is already Y-up; vanilla's scale(-1,-1,1) Y
        //    is for legacy Java entity models with Y-down convention).
        rotBuf.identity().rotationY((float) Math.toRadians(180.0f - state.bodyRot));
        poseStack.mulPose(rotBuf);
        poseStack.scale(PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE);

        RenderType type = RenderType.entityCutoutNoCull(state.texture);
        int packedOverlay = entity instanceof LivingEntity living
                ? net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(living, 0.0f)
                : OverlayTexture.NO_OVERLAY;
        VertexConsumer vc = bufferSource.getBuffer(type);
        mesh.render(model, poseStack, vc, packedLight, packedOverlay, poseBuf);

        if (!renderLayers.isEmpty()) {
            RenderLayerContext layerCtx = new RenderLayerContext(
                    model, poseBuf, state, poseStack, bufferSource, packedLight, packedOverlay);
            for (RenderLayer layer : renderLayers) {
                layer.submit(layerCtx);
            }
        }

        poseStack.popPose();
    }

    /**
     * Samples a slot into {@code poseBuf}. When the slot has a fade in
     * progress the previous channel is sampled into a temp buffer, the
     * current channel into another, and the two are blended via
     * {@link PoseMixer#blend}; otherwise the current channel writes directly.
     *
     * <p>Per-frame allocation of the temp buffers is intentional — the GUI
     * preview path may call submit twice in the same frame with different
     * pose buffers, and a renderer-shared scratch space would race.
     */
    private void sampleSlot(SlotState slot, long nowNs,
                            MolangContext ctx, float[] poseBuf, int boneCount) {
        if (slot == null || slot.isEmpty()) {
            return;
        }
        if (!slot.hasFade()) {
            PoseSampler.sample(slot.current(), nowNs, ctx, poseBuf);
            return;
        }

        float[] fromPose = new float[boneCount * PoseSampler.FLOATS_PER_BONE];
        float[] toPose = new float[boneCount * PoseSampler.FLOATS_PER_BONE];
        PoseSampler.resetIdentity(fromPose, boneCount);
        PoseSampler.resetIdentity(toPose, boneCount);

        PoseSampler.sample(slot.previous(), nowNs, ctx, fromPose);
        PoseSampler.sample(slot.current(), nowNs, ctx, toPose);
        float alpha = PoseMixer.fadeAlpha(slot.fadeStartNs(), slot.fadeDurationSec(), nowNs);
        PoseMixer.blend(fromPose, toPose, alpha, poseBuf, boneCount);
    }
}
