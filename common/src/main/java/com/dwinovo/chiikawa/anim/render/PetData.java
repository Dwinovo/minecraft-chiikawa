package com.dwinovo.chiikawa.anim.render;

/**
 * Typed-map keys for {@link ChiikawaRenderState} extras.
 *
 * <p>Per-frame extension data (held item, mood, current emote, prop bindings,
 * spring-bone state, ...) lives in a fixed-size {@code Object[]} on the render
 * state, indexed by these enum constants. Adding a new key is append-only:
 * declare a constant, populate it in {@code extractRenderState}, read it from
 * the consuming layer/interceptor.
 *
 * <p>This intentionally avoids the full Geckolib {@code DataTicket<T>}
 * apparatus — there are no platform events or cross-mod data sharing
 * concerns for a single-mod codebase. The trade-off is unchecked casts on
 * read, which the {@code @SuppressWarnings("unchecked")} on
 * {@link ChiikawaRenderState#get(PetData)} concentrates in one place.
 *
 * <p>Stable order; do not reorder existing constants — the ordinals back the
 * extras array index. Append new constants at the end.
 */
public enum PetData {
    /** Mainhand {@link net.minecraft.world.item.ItemStack} snapshot consumed by {@code HeldItemLayer}. */
    HELD_ITEM_STACK;

    /** Cached values() to avoid per-frame allocation; treat as immutable. */
    public static final PetData[] VALUES = values();
}
