package com.dwinovo.chiikawa.entity.brain.errand.rest;

import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * What a rest is about, for a source that depends on it and for a rest that is on one thing and not on the errand:
 * the camera item a photo is taken with, or the scene two pets play and the pet it is played with.
 *
 * @param key the id of that item or scene, when the rest is about one
 * @param subject what the rest is on when its spec says it is on one thing, see {@link Plan#subject}
 * @param pastimes how much more or less often than the data says the world has the pets pass the time, which a rest between
 *                 two pastimes is divided by; the ledger sets what the world says as it works a rest out, see
 *                 {@link #inWorldOf}
 */
public record RestContext(Optional<ResourceLocation> key, Optional<String> subject, float pastimes) {
    /** A rest about nothing in particular. */
    public static final RestContext NONE = new RestContext(Optional.empty(), Optional.empty(), 1.0F);

    /** @return the context of a rest about this item or scene */
    public static RestContext of(ResourceLocation key) {
        return new RestContext(Optional.of(key), Optional.empty(), 1.0F);
    }

    /** @return the context of a rest about what this plan is about */
    public static RestContext of(Plan plan) {
        return new RestContext(plan.restKey(), plan.subject(), 1.0F);
    }

    /** @return this context in a world that has the pastimes this much as the data says */
    public RestContext inWorldOf(float pastimes) {
        return new RestContext(key, subject, pastimes);
    }
}
