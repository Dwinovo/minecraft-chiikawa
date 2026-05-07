package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import net.minecraft.resources.ResourceLocation;

/**
 * Render-state snapshot for chiikawa-animated entities on the pre-render-state
 * Minecraft renderer API. The renderer fills this object directly from the
 * live entity each frame.
 *
 * <p>Animation timing is captured as a snapshot of the entity's
 * {@link AnimationChannel} records ({@link AnimationChannel} is immutable, so
 * this is a safe shallow copy). The actual pose is sampled in
 * {@link ChiikawaEntityRenderer#render} via the pure-function
 * {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler} — extract carries no
 * mutable cursor that could double-step on a second extract call.
 *
 * <h2>Extras</h2>
 * Layer- and interceptor-specific data goes through the {@link PetData}
 * typed map ({@link #put} / {@link #get}) rather than direct fields. Adding a
 * new piece of state means appending a {@link PetData} constant — the render
 * state class itself stays untouched.
 */
public class ChiikawaRenderState {
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
    /** Body yaw in degrees. */
    public float bodyRot;
    /** Entity age plus partial tick. */
    public float ageInTicks;
    /**
     * Head yaw relative to body, in degrees, captured at extract time.
     *
     * <p>Stored here rather than recomputed at render time so inventory and
     * world rendering both use the entity's real head turn snapshot.
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
