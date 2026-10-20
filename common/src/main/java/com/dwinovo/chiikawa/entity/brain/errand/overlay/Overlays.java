package com.dwinovo.chiikawa.entity.brain.errand.overlay;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.run.Overlay;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The overlays an errand's settings can name, by id: {@code "overlays": ["chiikawa:idle_chatter"]}. The names
 * are fixed here and checked when an errand file is loaded: an errand naming one that is not on this list is
 * turned off.
 */
public final class Overlays {
    /** Saying something now and then. */
    public static final ResourceLocation IDLE_CHATTER = id("idle_chatter");

    private static final Map<ResourceLocation, Overlay> BY_ID = Map.of(IDLE_CHATTER, new IdleChatter());

    private Overlays() {
    }

    /** @return the overlay with this id */
    public static Optional<Overlay> get(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
