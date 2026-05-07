package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Render-state snapshot for chiikawa-animated entities. Extends vanilla
 * {@link LivingEntityRenderState} (so all standard fields are populated by
 * the vanilla extract pipeline) and adds the model / texture identity that
 * the renderer uses to look up baked data and bind a texture.
 *
 * <p>Animation timing is captured as a snapshot of the entity's
 * {@link AnimationChannel} records ({@link AnimationChannel} is immutable, so
 * this is a safe shallow copy). The actual pose is sampled in
 * {@link ChiikawaEntityRenderer#submit} via the pure-function
 * {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler} — extract carries no
 * mutable cursor that could double-step on a second extract call.
 *
 * <h2>Extras</h2>
 * Layer- and interceptor-specific data goes through the {@link PetData}
 * typed map ({@link #put} / {@link #get}) rather than direct fields. Adding a
 * new piece of state means appending a {@link PetData} constant — the render
 * state class itself stays untouched.
 */
public class ChiikawaRenderState extends LivingEntityRenderState {
    /** Resource key under which the model was registered in {@link com.dwinovo.chiikawa.anim.api.ModelLibrary}. */
    public ResourceLocation modelKey;
    /** Texture path. */
    public ResourceLocation texture;
    /** Snapshot of the BASE-slot (main) animation channel. {@code null} if nothing is playing. */
    public AnimationChannel mainChannel;
    /** Snapshots of any non-BASE triggered channels. {@code null} entries are skipped. */
    public AnimationChannel[] subChannels;
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
