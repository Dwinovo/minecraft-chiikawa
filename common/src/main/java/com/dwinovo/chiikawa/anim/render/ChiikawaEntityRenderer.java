package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.api.AnimationLibrary;
import com.dwinovo.chiikawa.anim.api.ChiikawaAnimated;
import com.dwinovo.chiikawa.anim.api.ModelLibrary;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.dwinovo.chiikawa.anim.controller.BlendMode;
import com.dwinovo.chiikawa.anim.controller.ControllerConfig;
import com.dwinovo.chiikawa.anim.controller.ControllerHandler;
import com.dwinovo.chiikawa.anim.controller.ControllerRoster;
import com.dwinovo.chiikawa.anim.pose.BasicMolangInputProvider;
import com.dwinovo.chiikawa.anim.pose.BoneInterceptor;
import com.dwinovo.chiikawa.anim.pose.BoneVisibility;
import com.dwinovo.chiikawa.anim.pose.BoneVisibilityRule;
import com.dwinovo.chiikawa.anim.pose.HeadLookInterceptor;
import com.dwinovo.chiikawa.anim.pose.PoseComposer;
import com.dwinovo.chiikawa.anim.render.layer.HeldItemLayer;
import com.dwinovo.chiikawa.client.voice.PetIconRenderer;
import com.dwinovo.chiikawa.client.voice.SpeechBubbleRenderer;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.item.BagItem;
import com.dwinovo.chiikawa.anim.render.layer.BagLayer;
import com.dwinovo.chiikawa.anim.render.layer.RenderLayer;
import com.dwinovo.chiikawa.anim.render.layer.RenderLayerContext;
import com.dwinovo.chiikawa.anim.runtime.AnimationClock;
import com.dwinovo.chiikawa.anim.runtime.Animator;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import com.dwinovo.chiikawa.anim.state.PetAnimationResolver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Base entity renderer for Bedrock-format pets. Owns the per-pet controller
 * roster, the model lookup, and the Bedrock entity transform; the engine
 * ({@link Animator}, {@link PoseComposer}, {@link BoneVisibility}) advances the
 * controllers and composes the pose, this class draws it.
 *
 * <h2>Controllers</h2>
 * Subclasses register {@link ControllerConfig}s in their constructor. The
 * base class registers a single state-driven {@code "main"} controller that
 * resolves a base loop via {@link PetAnimationResolver}, plus the
 * {@code "action"}, {@code "reaction"} and {@code "talk"} dummy controllers that
 * {@link com.dwinovo.chiikawa.entity.AbstractPet} dispatches one-shot
 * animations to. Subclasses append decorative controllers (blink, breath, ...)
 * via {@link #addController}.
 *
 * <p>Controllers run in registration order; later controllers' samples
 * compose on top of earlier ones in the shared pose buffer per their
 * {@link BlendMode}.
 *
 * <h2>ResourceLocation convention</h2>
 * {@code modelKey} is {@code namespace:path} (e.g. {@code chiikawa:chiikawa}).
 * The matching texture is at {@code textures/entities/<path>.png}, animation
 * names register at {@code <ns>:<path>/<anim_name>}.
 */
public abstract class ChiikawaEntityRenderer<T extends Entity> extends EntityRenderer<T, ChiikawaRenderState> {

    private static final float PIXEL_SCALE = 1.0f / 16.0f;
    /** Default base-loop animation name. */
    private static final String DEFAULT_LOOP_NAME = "idle";

    /** Controller name carrying state-driven base loop (idle/walk/run/sit/...). */
    public static final String CONTROLLER_MAIN = "main";
    /** Controller name receiving one-shot action triggers from {@link com.dwinovo.chiikawa.entity.AbstractPet}. */
    public static final String CONTROLLER_ACTION = "action";
    /** Controller name receiving one-shot reaction triggers. */
    public static final String CONTROLLER_REACTION = "reaction";
    /** Controller name the pet opens its mouth on while it says something. */
    public static final String CONTROLLER_TALK = "talk";
    /**
     * Controller name driving the ambient blink loop. Plays the {@code blink}
     * animation from each pet's animation file as a constant decorative loop.
     * Pets without a {@code blink} animation get a no-op (handler returns
     * {@code null} and the controller stays silent) — graceful degradation,
     * no per-pet code needed.
     */
    public static final String CONTROLLER_BLINK = "blink";
    /** Pets are 0.6 blocks wide; this matches vanilla small-mob shadow sizing. */
    private static final float DEFAULT_SHADOW_RADIUS = 0.4f;
    /**
     * Stop-fade duration applied to action / reaction controllers when their
     * triggered animation finishes. Lets the bones the trigger animated blend
     * smoothly back to whatever main wrote, instead of snapping in a single
     * frame. Trigger fade-<em>in</em> stays disabled (gameplay events should
     * feel responsive); only fade-<em>out</em> uses this value.
     */
    private static final float TRIGGER_STOP_FADE_SEC = 0.15f;
    /** Crossfade of the state-driven main controller between base loops. */
    private static final float MAIN_TRANSITION_SEC = 0.16f;
    /** The bone the head look-at turns, in every pet's model. */
    private static final String HEAD_BONE = "AllHead";

    protected final ResourceLocation modelKey;
    protected final ResourceLocation textureLocation;
    protected final ResourceLocation defaultLoopKey;

    private final ModelRenderer mesh = new ModelRenderer();
    private final Quaternionf rotBuf = new Quaternionf();
    /**
     * Per-pet controller registry, declared in subclass constructors and
     * frozen once the renderer is built. The same roster is handed to every
     * entity's {@link Animator} on first extract — controllers themselves
     * are stateless config; per-entity mutable state lives in
     * {@link com.dwinovo.chiikawa.anim.controller.ControllerInstance}.
     */
    private final ControllerRoster<PetAnimContext> controllers = new ControllerRoster<>();
    /** Turns the controllers' snapshots into the pose the model is drawn from. */
    private final PoseComposer poseComposer = new PoseComposer();
    /**
     * Visual layers that run after the main mesh submits, in registration order.
     * Subclasses register additional layers (props, emissive overlays, capes...)
     * via {@link #addRenderLayer(RenderLayer)} from their constructor.
     */
    private final List<RenderLayer> renderLayers = new ArrayList<>();
    /**
     * Per-bone visibility rules. A bone with one or more rules is visible
     * when <em>any</em> rule votes {@code true}; absence of a vote means the
     * bone is hidden that frame. Bones never registered are always visible.
     */
    private final BoneVisibility<ChiikawaRenderState> visibility = new BoneVisibility<>();

    protected ChiikawaEntityRenderer(EntityRendererProvider.Context ctx, String name) {
        super(ctx);
        this.shadowRadius = DEFAULT_SHADOW_RADIUS;
        this.modelKey = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name);
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                "textures/entities/" + name + ".png");
        this.defaultLoopKey = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                name + "/" + DEFAULT_LOOP_NAME);

        // Default MoLang inputs.
        poseComposer.addInputProvider(new BasicMolangInputProvider());
        // Default interceptors shared by all pets.
        // Head look-at is the only universal procedural override — every pet
        // tracks the player's view direction. Ear / tail sway used to live on
        // a hardcoded PHYSICS_SECONDARY interceptor; it was removed once
        // animator-authored ear/tail keyframes (in idle/walk/etc.) became the
        // single source of truth, so animation data isn't silently overwritten.
        poseComposer.addInterceptor(BoneInterceptor.Stage.LOOK_AT, new HeadLookInterceptor(HEAD_BONE));
        // Default layers shared by all pets.
        addRenderLayer(new HeldItemLayer(PropBone.bones()));
        addRenderLayer(new BagLayer());
        // A bag's strap is part of the pet's own model, there only while a bag worn
        // that way is on.
        for (BagItem.Wear wear : BagItem.Wear.values()) {
            addBoneVisibilityRule(wear.strap(), state -> BagLayer.wearOf(state) == wear);
        }
        // What a pet has with it only while it does something, likewise part of its model.
        for (PropBone prop : PropBone.values()) {
            addBoneVisibilityRule(prop.bone(), prop.shownDuring());
        }

        // Default controllers shared by all pets, in priority-from-low order:
        //   "main"     state-driven base loop
        //   "blink"    ambient eye-blink decorative loop (graceful no-op when
        //              the pet's animation file has no "blink" animation)
        //   "action"   externally-triggered one-shot semantic actions — runs
        //              after blink so a gameplay action's eye keyframes (if
        //              any) override ambient blinking
        //   "reaction" externally-triggered one-shot emotional reactions —
        //              same priority rationale as action
        //   "talk"     the mouth opening while the pet says a line — after
        //              reaction, whose face arrives in the same tick and would
        //              otherwise replace it; it keys only the mouth, so the rest
        //              of the face stays as the reaction left it
        // Sub-classes append further decorative or override controllers on top.
        addController(new ControllerConfig<>(
                CONTROLLER_MAIN,
                BlendMode.OVERRIDE,
                MAIN_TRANSITION_SEC,
                this::resolveMainAnimation));
        addLoopingController(CONTROLLER_BLINK, BlendMode.OVERRIDE, "blink");
        addController(new ControllerConfig<>(
                CONTROLLER_ACTION,
                BlendMode.OVERRIDE,
                TRIGGER_STOP_FADE_SEC,
                ControllerHandler::neverPlay));
        addController(new ControllerConfig<>(
                CONTROLLER_REACTION,
                BlendMode.OVERRIDE,
                TRIGGER_STOP_FADE_SEC,
                ControllerHandler::neverPlay));
        addController(new ControllerConfig<>(
                CONTROLLER_TALK,
                BlendMode.OVERRIDE,
                TRIGGER_STOP_FADE_SEC,
                ControllerHandler::neverPlay));
    }

    /** Register an additional {@link RenderLayer}; runs after the main mesh in registration order. */
    protected final void addRenderLayer(RenderLayer layer) {
        renderLayers.add(layer);
    }

    /**
     * Append a controller to the per-entity roster. Registration order is
     * priority — controllers added later override / blend on top of earlier
     * ones in the shared pose buffer.
     *
     * <p>Two controllers must not share a name. Names are used by
     * {@link Animator#playOnce} to dispatch external one-shot triggers.
     */
    protected final void addController(ControllerConfig<PetAnimContext> config) {
        controllers.add(config);
    }

    /**
     * Helper for subclasses to register a constant looping controller — the
     * common "blink / breath / tail-idle" pattern. The handler is a closure
     * over the animation lookup so a missing anim file (during dev) doesn't
     * crash the renderer.
     */
    protected final void addLoopingController(String name, BlendMode blendMode, String animationName) {
        ResourceLocation key = animKey(animationName);
        addController(new ControllerConfig<>(name, blendMode, 0f, ctx -> AnimationLibrary.get(key)));
    }

    /**
     * Register a per-frame visibility predicate for a named bone. The bone
     * (and its entire subtree) is hidden when no registered rule for that
     * bone votes visible.
     *
     * <p>This is the long-term home for "this bone shows when X" logic — see
     * {@link BoneVisibilityRule} for the spec / Bedrock-compatibility
     * justification. Registering multiple rules for the same bone composes
     * with OR semantics (any vote shows the bone).
     *
     * <p>Typical usage in a subclass constructor:
     * <pre>{@code
     * addBoneVisibilityRule("guitar",
     *     state -> state.pose.isPlaying(CONTROLLER_ACTION, "play_guitar"));
     * }</pre>
     */
    protected final void addBoneVisibilityRule(String boneName, BoneVisibilityRule<ChiikawaRenderState> rule) {
        visibility.add(boneName, rule);
    }

    /**
     * Visibility-rule helper: whether the pet is talking, i.e. one of the
     * {@link PetSpeech#MOUTHS} animations is playing. A face whose mouth opens as
     * Chiikawa's does shows its opening ({@code Mouth3}) only then.
     */
    public static boolean isTalking(ChiikawaRenderState state) {
        for (String mouth : PetSpeech.MOUTHS) {
            if (state.pose.isAnyPlaying(mouth)) {
                return true;
            }
        }
        return false;
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
            state.pose.walkSpeed = living.walkAnimation.speed(partialTick);
            state.hasRedOverlay = living.hurtTime > 0 || living.deathTime > 0;
            state.deathTime = living.deathTime > 0 ? living.deathTime + partialTick : 0.0f;
            // Capture the head-look snapshot here so it survives the post-extract
            // bodyRot/yRot stomp performed by InventoryScreen.renderEntityInInventoryFollowsMouse.
            state.pose.netHeadYaw = net.minecraft.util.Mth.wrapDegrees(headRot - bodyRot);
            state.pose.headPitch  = pitch;

            // Stash the live mainhand stack. HeldItemLayer resolves it into
            // a fresh ItemStackRenderState while rendering. Lives on the typed
            // extras map so future layers can register their own keys without
            // adding fields to ChiikawaRenderState.
            state.put(PetData.HELD_ITEM_STACK, living.getMainHandItem());
        }

        if (entity instanceof AbstractPet pet) {
            state.put(PetData.WORN_BAG, pet.getItemBySlot(EquipmentSlot.CHEST));
        }

        // Taken with the rest of the state, as a name tag's text is; a pet drawn as a picture
        // in a screen says nothing over its head.
        state.put(PetData.SPEECH, !drawingPortrait && entity instanceof AbstractPet pet
            ? speech.extract(pet, partialTick) : null);
        state.put(PetData.ICON, !drawingPortrait && entity instanceof AbstractPet pet
            ? icon.extract(pet, partialTick) : null);

        if (entity instanceof ChiikawaAnimated animated) {
            long nowNs = AnimationClock.fromTicks(entity.tickCount, partialTick);
            state.pose.animationTimeNs = nowNs;

            // A controller still holding a pre-reload BakedAnimation is dropped by the animator.
            BakedModel currentModel = ModelLibrary.get(modelKey);
            long bakeStamp = currentModel == null ? 0L : currentModel.bakeStamp;
            state.pose.controllerSnapshots = animated.getPetAnimator().advance(controllers,
                    animated.getAnimContext(state.pose.walkSpeed), entity.getUUID().getLeastSignificantBits(),
                    nowNs, bakeStamp);
            state.hiddenBones = visibility.evaluate(currentModel, state);
        }
    }

    @Override
    protected float getShadowRadius(ChiikawaRenderState state) {
        return super.getShadowRadius(state) * state.scale;
    }

    /**
     * Default {@code "main"} controller handler — picks the base loop via
     * {@link PetAnimationResolver} with a graceful fallback to
     * {@code "<modelKey>/idle"} when none of the resolver's candidates exist
     * yet (common during dev when only some animations are authored).
     */
    protected BakedAnimation resolveMainAnimation(PetAnimContext ctx) {
        BakedAnimation wanted = firstAvailable(PetAnimationResolver.resolve(ctx));
        if (wanted != null) return wanted;
        return AnimationLibrary.get(defaultLoopKey);
    }

    /** Resolve {@code <modelKey>:<animName>} — the global animation library key for this pet. */
    protected final ResourceLocation animKey(String name) {
        return ResourceLocation.fromNamespaceAndPath(modelKey.getNamespace(),
                modelKey.getPath() + "/" + name);
    }

    /** First animation in the candidate list that exists in the library, or {@code null}. */
    protected final BakedAnimation firstAvailable(Iterable<String> names) {
        for (String name : names) {
            BakedAnimation animation = AnimationLibrary.get(animKey(name));
            if (animation != null) return animation;
        }
        return null;
    }

    /** Whether a pet is being drawn as a picture in a screen rather than in the world. */
    private static boolean drawingPortrait;

    /** What the pet is saying, over its head. */
    private final SpeechBubbleRenderer speech = new SpeechBubbleRenderer();
    /** What is on its mind, over its head when it is not saying anything. */
    private final PetIconRenderer icon = new PetIconRenderer();

    /**
     * Draws a pet as a picture in a screen: the model, and nothing over its head. When its
     * screen opens the crosshair is on it, so a named pet would carry its name tag into the
     * picture, and a talking one its bubble; the screen says all that already.
     */
    public static void drawPortrait(Runnable draw) {
        drawingPortrait = true;
        try {
            draw.run();
        } finally {
            drawingPortrait = false;
        }
    }

    @Override
    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return !drawingPortrait && super.shouldShowName(entity, distanceToCameraSq);
    }

    @Override
    public void render(ChiikawaRenderState state, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        BakedModel model = ModelLibrary.get(state.modelKey);
        if (model == null) {
            renderOverHead(state, poseStack, bufferSource, packedLight);
            return;
        }

        float[] poseBuf = poseComposer.compose(model, state.pose);

        poseStack.pushPose();
        // Bedrock entity rendering — see ModelBaker for the X-mirror reasoning.
        rotBuf.identity().rotationY((float) Math.toRadians(180.0f - state.bodyRot));
        poseStack.last().rotate(rotBuf);
        poseStack.scale(PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE);

        RenderType type = RenderType.entityCutoutNoCull(state.texture);
        int packedOverlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(state, 0.0f);
        VertexConsumer vc = bufferSource.getBuffer(type);
        boolean[] hiddenBones = state.hiddenBones;
        mesh.render(model, poseStack, vc, packedLight, packedOverlay, poseBuf, hiddenBones);

        if (!renderLayers.isEmpty()) {
            RenderLayerContext layerCtx = new RenderLayerContext(
                    model, poseBuf, state, poseStack, bufferSource, packedLight, packedOverlay);
            for (RenderLayer layer : renderLayers) {
                layer.submit(layerCtx);
            }
        }

        poseStack.popPose();
        renderOverHead(state, poseStack, bufferSource, packedLight);
    }

    /** The name tag, as vanilla draws it, and what the pet is saying or has on its mind above it. */
    private void renderOverHead(ChiikawaRenderState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight) {
        super.render(state, poseStack, bufferSource, packedLight);
        SpeechBubbleRenderer.Saying saying = state.get(PetData.SPEECH);
        if (saying != null) {
            speech.render(saying, poseStack, bufferSource, getFont(), entityRenderDispatcher.cameraOrientation());
        }
        PetIconRenderer.Mind mind = state.get(PetData.ICON);
        if (mind != null) {
            icon.render(mind, poseStack, bufferSource, getFont(), entityRenderDispatcher.cameraOrientation());
        }
    }
}
