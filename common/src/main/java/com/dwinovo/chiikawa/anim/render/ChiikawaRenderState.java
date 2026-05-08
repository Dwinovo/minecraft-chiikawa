package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.controller.ControllerSnapshot;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Render-state snapshot for chiikawa-animated entities. Extends vanilla
 * {@link LivingEntityRenderState} (so all standard fields are populated by
 * the vanilla extract pipeline) and adds the model / texture identity that
 * the renderer uses to look up baked data and bind a texture.
 *
 * <h2>Animation snapshot</h2>
 * The animator's per-controller state is captured as a
 * {@link ControllerSnapshot} array in registration order — controllers run
 * later in the array compose on top of earlier ones via the renderer's submit
 * loop. Putting an immutable snapshot here (rather than the live animator
 * reference) keeps the submit pass a pure function of render-state plus baked
 * data: a second extract for an inventory preview snapshots fresh values
 * without disturbing the world-render submit.
 *
 * <h2>Extras</h2>
 * Layer- and interceptor-specific data goes through the {@link PetData}
 * typed map ({@link #put} / {@link #get}) rather than direct fields. Adding a
 * new piece of state means appending a {@link PetData} constant — the render
 * state class itself stays untouched.
 */
public class ChiikawaRenderState extends LivingEntityRenderState {
    /** Identifier under which the model was registered in {@link com.dwinovo.chiikawa.anim.api.ModelLibrary}. */
    public Identifier modelKey;
    /** Texture path. */
    public Identifier texture;
    /**
     * Per-controller snapshot in registration order. The submit pass iterates
     * this and samples each controller into the shared pose buffer using its
     * declared {@link com.dwinovo.chiikawa.anim.controller.BlendMode}.
     */
    public ControllerSnapshot[] controllerSnapshots = EMPTY;
    /**
     * Per-bone hidden flag, parallel to the model's bone array
     * (length {@code = bones.length}, indexed by bone index). Computed at
     * extract time by evaluating any registered
     * {@link BoneVisibilityRule}s. {@code true} = the bone (and its entire
     * subtree) should be skipped during rendering.
     *
     * <p>Empty (zero-length) when the renderer has no visibility rules
     * registered — saves the per-frame allocation in the common case.
     */
    public boolean[] hiddenBones = EMPTY_HIDDEN;
    /** {@code walkAnimation.speed(partialTick)} — feeds Molang {@code query.ground_speed}. */
    public float walkSpeed;
    /**
     * Head yaw relative to body, in degrees, captured at extract time.
     *
     * <p>Stored here rather than recomputed from {@link #yRot} − {@link #bodyRot}
     * at submit time because {@link net.minecraft.client.gui.screens.inventory.InventoryScreen#renderEntityInInventoryFollowsMouse}
     * <em>overwrites</em> {@code bodyRot} / {@code yRot} after extract finishes
     * (it sets {@code yRot = f * 20}, {@code bodyRot = 180 + f * 20}, giving a
     * −180 difference that has nothing to do with the entity's real head turn).
     * Snapshot during extract so submit never has to infer it from display
     * rotations.
     */
    public float netHeadYaw;
    /** Head pitch (entity X rotation) in degrees, captured at extract time for the same reason. */
    public float headPitch;

    private static final ControllerSnapshot[] EMPTY = new ControllerSnapshot[0];
    private static final boolean[] EMPTY_HIDDEN = new boolean[0];

    private final Object[] extras = new Object[PetData.VALUES.length];

    /** Stash a value for the given {@link PetData} key; {@code null} clears the slot. */
    public <T> void put(PetData key, T value) {
        extras[key.ordinal()] = value;
    }

    /** Reads the value for the given {@link PetData} key, or {@code null} if absent. */
    @SuppressWarnings("unchecked")
    public <T> T get(PetData key) {
        return (T) extras[key.ordinal()];
    }
}
