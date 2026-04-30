package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Render-state snapshot for chiikawa-animated entities. Older Minecraft
 * versions do not expose vanilla entity render states, so this class carries
 * just the fields needed by the Bedrock renderer.
 *
 * <p>Animation timing is captured as a snapshot of the entity's
 * {@link AnimationChannel} records ({@link AnimationChannel} is immutable, so
 * this is a safe shallow copy). The actual pose is sampled in
 * {@link ChiikawaEntityRenderer#render} via the pure-function
 * {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler} — extract carries no
 * mutable cursor that could double-step on a second extract call.
 */
public class ChiikawaRenderState {
    /** Resource key under which the model was registered in {@link com.dwinovo.chiikawa.anim.api.ModelLibrary}. */
    public ResourceLocation modelKey;
    /** Texture path. */
    public ResourceLocation texture;
    /** Snapshot of layer-0 (main) animation channel. {@code null} if nothing is playing. */
    public AnimationChannel mainChannel;
    /** Snapshots of any non-main triggered channels. {@code null} entries are skipped. */
    public AnimationChannel[] subChannels;
    /** {@code walkAnimation.speed(partialTick)} — feeds Molang {@code query.ground_speed}. */
    public float walkSpeed;
    /** Body yaw in degrees. */
    public float bodyRot;
    /** Render age in ticks. */
    public float ageInTicks;
    /**
     * Head yaw relative to body, in degrees, captured at extract time.
     *
     * <p>Stored here rather than recomputed during drawing so GUI/entity
     * preview transforms cannot stomp the real head turn.
     */
    public float netHeadYaw;
    /** Head pitch (entity X rotation) in degrees, captured at extract time for the same reason. */
    public float headPitch;

    /** Mainhand item snapshot rendered while drawing attachments. */
    public ItemStack heldItemStack = ItemStack.EMPTY;
}
