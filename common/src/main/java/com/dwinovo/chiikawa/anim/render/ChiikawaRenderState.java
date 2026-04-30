package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.runtime.AnimationChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Render-state snapshot for chiikawa-animated entities on the pre-render-state
 * Minecraft renderer API. The renderer fills this object directly from the
 * live entity each frame.
 *
 * <p>Animation timing is captured as a snapshot of the entity's
 * {@link AnimationChannel} records ({@link AnimationChannel} is immutable, so
 * this is a safe shallow copy). The actual pose is sampled through
 * {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler}.
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

    /** Mainhand item snapshot rendered at the held-item bone. */
    public ItemStack heldItemStack = ItemStack.EMPTY;
}
