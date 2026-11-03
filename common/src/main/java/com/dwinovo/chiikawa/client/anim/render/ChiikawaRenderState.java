package com.dwinovo.chiikawa.client.anim.render;

import com.dwinovo.chiikawa.anim.pose.PoseFrame;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Render-state snapshot for chiikawa-animated entities. Extends vanilla
 * {@link LivingEntityRenderState} (so all standard fields are populated by
 * the vanilla extract pipeline) and adds the model / texture identity that
 * the renderer uses to look up baked data and bind a texture.
 *
 * <h2>Animation snapshot</h2>
 * What the engine reads to pose the entity is captured in {@link #pose}: the animator's
 * per-controller state as an immutable snapshot (rather than the live animator
 * reference), the clock, and the few numbers its interceptors feed on. That keeps the submit
 * pass a pure function of render-state plus baked data: a second extract for an
 * inventory preview snapshots fresh values without disturbing the world-render submit.
 *
 * <h2>Extras</h2>
 * Layer- and interceptor-specific data goes through the {@link PetData}
 * typed map ({@link #put} / {@link #get}) rather than direct fields. Adding a
 * new piece of state means appending a {@link PetData} constant — the render
 * state class itself stays untouched.
 */
public class ChiikawaRenderState extends LivingEntityRenderState {
    /** Identifier under which the model was registered in {@link com.dwinovo.chiikawa.client.anim.ModelLibrary}. */
    public Identifier modelKey;
    /** Texture path. */
    public Identifier texture;
    /**
     * What the engine poses the entity from, captured during extract: the controller snapshots
     * in registration order, the pause-aware animation time, walk speed, and head look.
     * The submit pass composes the entity's pose from it.
     *
     * <p>The head look is taken here, during extract, rather than worked out from
     * {@link #yRot} − {@link #bodyRot} at submit time: {@link net.minecraft.client.gui.screens.inventory.InventoryScreen#extractEntityInInventoryFollowsMouse}
     * <em>overwrites</em> {@code bodyRot} / {@code yRot} after extract finishes, giving a
     * difference that has nothing to do with the entity's real head turn.
     */
    public final PoseFrame pose = new PoseFrame();
    /**
     * Per-bone hidden flag, parallel to the model's bone array
     * (length {@code = bones.length}, indexed by bone index). Computed at
     * extract time by evaluating any registered
     * {@link com.dwinovo.chiikawa.anim.pose.BoneVisibilityRule}s. {@code true} = the bone (and its entire
     * subtree) should be skipped during rendering.
     *
     * <p>Empty (zero-length) when the renderer has no visibility rules
     * registered — saves the per-frame allocation in the common case.
     */
    public boolean[] hiddenBones = EMPTY_HIDDEN;

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
