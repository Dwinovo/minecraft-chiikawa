package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.PetInteractions;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * What the two errands of a scene, going over to another pet and playing along with one that comes, have in common:
 * how long the pair leave a scene alone afterwards, which is a number of the scene and so lives in its file.
 */
final class Scenes {
    private Scenes() {
    }

    /** @return the named numbers a scene's rest points at: its cooldown, for the scene the rest is about */
    static Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Map.of(RestSources.SCENE_COOLDOWN, context -> context.key().map(PetInteractions.all()::get)
            .map(PetInteraction::cooldownTicks).map(Integer::longValue).orElse(0L));
    }
}
