package com.dwinovo.chiikawa.entity.brain.errand.rest;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * What a rest is about, for a source that depends on it: the camera item a photo is taken with,
 * or the scene two pets play.
 *
 * @param key the id of that item or scene, when the rest is about one
 */
public record RestContext(Optional<ResourceLocation> key) {
    /** A rest about nothing in particular. */
    public static final RestContext NONE = new RestContext(Optional.empty());

    /** @return the context of a rest about this item or scene */
    public static RestContext of(ResourceLocation key) {
        return new RestContext(Optional.of(key));
    }
}
