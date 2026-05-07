package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import com.dwinovo.chiikawa.anim.runtime.SlotState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

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
    /** Identifier under which the model was registered in {@link com.dwinovo.chiikawa.anim.api.ModelLibrary}. */
    public Identifier modelKey;
    /** Texture path. */
    public Identifier texture;
    /** Snapshot of the BASE slot's state (current channel + optional fade-from). */
    public SlotState mainSlot;
    /** Snapshots of any non-BASE slots. {@code null} entries are skipped. */
    public SlotState[] subSlots;
    /**
     * Snapshot of the model's parallel tracks (blink, breath, ...), each
     * already resolved against the {@link com.dwinovo.chiikawa.anim.api.AnimationLibrary}
     * with the entity's stable phase seed as {@code startTimeNs}. Sampled
     * <em>after</em> {@link #mainSlot} and {@link #subSlots} so they win on
     * shared bones, matching YSM's post-parallel semantic.
     *
     * <p>{@code null} when the model declares no parallel tracks or none could
     * be resolved.
     */
    public AnimationChannel[] parallelChannels;
    /** {@code walkAnimation.speed(partialTick)} — feeds Molang {@code query.ground_speed}. */
    public float walkSpeed;
    /**
     * Head yaw relative to body, in degrees, captured at extract time.
     *
     * <p>Stored here rather than recomputed from {@link #yRot} − {@link #bodyRot}
     * at submit time because {@link net.minecraft.client.gui.screens.inventory.InventoryScreen#extractEntityInInventoryFollowsMouse}
     * <em>overwrites</em> {@code bodyRot} / {@code yRot} after extract finishes
     * (it sets {@code yRot = f * 20}, {@code bodyRot = 180 + f * 20}, giving a
     * −180 difference that has nothing to do with the entity's real head turn).
     * Snapshot this value during extract so submit uses the live entity state.
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
