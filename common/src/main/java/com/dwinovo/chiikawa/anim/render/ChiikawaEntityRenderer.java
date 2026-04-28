package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.AnimationLibrary;
import com.dwinovo.chiikawa.anim.api.ChiikawaAnimated;
import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import com.dwinovo.chiikawa.anim.runtime.PetAnimator;
import com.dwinovo.chiikawa.anim.runtime.PoseSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * Base entity renderer for Bedrock-format pets. Handles model lookup, the
 * Bedrock entity transform (Y rotation + pixel scale), and per-frame pose
 * sampling.
 *
 * <p>{@code modelKey} is the {@code namespace:path} address under which the
 * baked model was registered (e.g. {@code chiikawa:chiikawa}); the matching
 * texture is resolved at submit time from the {@code textureLocation} field,
 * and the default loop animation is registered as
 * {@code <modelKey>/<DEFAULT_LOOP_NAME>} (e.g. {@code chiikawa:chiikawa/idle}).
 */
public abstract class ChiikawaEntityRenderer<T extends Entity> extends EntityRenderer<T, ChiikawaRenderState> {

    private static final float PIXEL_SCALE = 1.0f / 16.0f;
    /** Animation name played on the main channel when nothing else is set. */
    private static final String DEFAULT_LOOP_NAME = "idle";

    protected final Identifier modelKey;
    protected final Identifier textureLocation;
    protected final Identifier defaultLoopKey;

    /** Bone name on every pet model where the mainhand item attaches. */
    private static final String HELD_ITEM_BONE = "RightHandLocator";

    private final ModelRenderer mesh = new ModelRenderer();
    private final Quaternionf rotBuf = new Quaternionf();
    private final MolangContext molangCtx = new MolangContext();
    private final BoneInterceptor[] interceptors = { new PetBoneInterceptor() };
    private final BoneAttachmentLayer heldItemLayer = new BoneAttachmentLayer();

    protected ChiikawaEntityRenderer(EntityRendererProvider.Context ctx, String name) {
        super(ctx);
        this.modelKey = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
        this.textureLocation = Identifier.fromNamespaceAndPath(Constants.MOD_ID,
                "textures/entities/" + name + ".png");
        this.defaultLoopKey = Identifier.fromNamespaceAndPath(Constants.MOD_ID,
                name + "/" + DEFAULT_LOOP_NAME);
    }

    @Override
    public ChiikawaRenderState createRenderState() {
        return new ChiikawaRenderState();
    }

    @Override
    public void extractRenderState(T entity, ChiikawaRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.modelKey = modelKey;
        state.texture = textureLocation;

        // Body rotation copied from LivingEntity — vanilla LivingEntityRenderState
        // owns this normally but EntityRenderer.extractRenderState does not.
        if (entity instanceof LivingEntity living) {
            float bodyRot = net.minecraft.util.Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            float headRot = net.minecraft.util.Mth.rotLerp(partialTick, living.yHeadRotO, living.getYHeadRot());
            float pitch   = net.minecraft.util.Mth.rotLerp(partialTick, living.xRotO, living.getXRot());
            state.bodyRot = bodyRot;
            state.yRot = headRot;
            state.xRot = pitch;
            state.scale = living.getScale();
            state.ageInTicks = living.tickCount + partialTick;
            state.walkSpeed = living.walkAnimation.speed(partialTick);
            // Capture the head-look snapshot here so it survives the post-extract
            // bodyRot/yRot stomp performed by InventoryScreen.renderEntityInInventoryFollowsMouse.
            state.netHeadYaw = net.minecraft.util.Mth.wrapDegrees(headRot - bodyRot);
            state.headPitch  = pitch;

            // Stash the live mainhand stack — we resolve it into a fresh
            // ItemStackRenderState at submit time, matching GeckoLib's call
            // pattern exactly (per-frame fresh state, updateForTopItem, level
            // pulled from Minecraft, owner = null).
            state.heldItemStack = living.getMainHandItem();
        }

        if (entity instanceof ChiikawaAnimated animated) {
            PetAnimator animator = animated.getPetAnimator();
            // Pick the desired main loop based on entity state. setMain is
            // idempotent — switches only when the wanted animation actually
            // changes, so a second extractRenderState in the same frame
            // (InventoryScreen.renderEntityInInventoryFollowsMouse) does not
            // restart the timer.
            String wantedName = animated.getMainAnimationName(state.walkSpeed);
            BakedAnimation wanted = AnimationLibrary.get(animKey(wantedName));
            if (wanted == null) wanted = AnimationLibrary.get(defaultLoopKey);
            if (wanted != null) {
                animator.setMain(wanted, true);
            }
            state.mainChannel = animator.get(0);
            // Snapshot any non-main channels populated by trigger(). The
            // AnimationChannel records are immutable so this is a safe shallow
            // copy.
            AnimationChannel[] subs = null;
            for (int i = 1; i < PetAnimator.CHANNEL_COUNT; i++) {
                AnimationChannel sub = animator.get(i);
                if (sub == null) continue;
                if (subs == null) subs = new AnimationChannel[PetAnimator.CHANNEL_COUNT - 1];
                subs[i - 1] = sub;
            }
            state.subChannels = subs;
        }
    }

    private Identifier animKey(String name) {
        return Identifier.fromNamespaceAndPath(modelKey.getNamespace(),
                modelKey.getPath() + "/" + name);
    }

    @Override
    public void submit(ChiikawaRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        BakedModel model = ModelLibrary.get(state.modelKey);
        if (model == null) {
            return;
        }

        // Pose buffer is allocated per-submit because submitCustomGeometry may
        // defer the draw lambda; a renderer-shared buffer would be overwritten
        // by the next entity's submit before this one's lambda runs.
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
        if (state.mainChannel != null) {
            PoseSampler.sample(state.mainChannel, nowNs, molangCtx, poseBuf);
        }
        if (state.subChannels != null) {
            for (AnimationChannel sub : state.subChannels) {
                if (sub != null) PoseSampler.sample(sub, nowNs, molangCtx, poseBuf);
            }
        }
        // Procedural overrides (head look-at, ear sway, tail wag). Run after
        // sampling so they cleanly replace the animation's contribution to
        // the affected bones — same semantics as GeckoLib's
        // adjustModelBonesForRender hook.
        for (BoneInterceptor interceptor : interceptors) {
            interceptor.apply(model, state, molangCtx, poseBuf);
        }

        poseStack.pushPose();
        // Bedrock entity rendering — matches Blockbench display and GeckoLib output.
        //  - Model forward = -Z (Bedrock convention). bodyRot=0 = entity faces +Z
        //    (south), so rotateY(180 - bodyRot) aligns the model with world facing.
        //  - The X mirror needed to undo Blockbench's display→JSON X negation is
        //    applied at BAKE TIME inside ModelBaker (and AnimationBaker for animation
        //    deltas). The renderer therefore uses a clean positive pixel scale here.
        //  - Y is not flipped (Bedrock is already Y-up; vanilla's scale(-1,-1,1) Y
        //    is for legacy Java entity models with Y-down convention).
        rotBuf.identity().rotationY((float) Math.toRadians(180.0f - state.bodyRot));
        poseStack.last().rotate(rotBuf);
        poseStack.scale(PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE);

        RenderType type = RenderTypes.entityCutoutNoCull(state.texture);
        int packedLight = state.lightCoords;
        int packedOverlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(state, 0.0f);
        collector.submitCustomGeometry(poseStack, type, (drawPose, vc) -> {
            mesh.render(model, drawPose, vc, packedLight, packedOverlay, poseBuf);
        });

        heldItemLayer.submit(model, poseBuf, HELD_ITEM_BONE, poseStack, collector,
                state.heldItemStack, packedLight);

        poseStack.popPose();
    }
}
