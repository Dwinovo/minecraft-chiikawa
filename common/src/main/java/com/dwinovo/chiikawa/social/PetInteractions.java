package com.dwinovo.chiikawa.social;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The scenes currently loaded from data packs. Server-side only; replaced as a whole on
 * every data pack (re)load.
 */
public final class PetInteractions {
    private static volatile SortedMap<ResourceLocation, PetInteraction> byId = Collections.emptySortedMap();

    private PetInteractions() {
    }

    /** @return every loaded scene by id, in id order so which one a pet thinks of first does not depend on load order */
    public static SortedMap<ResourceLocation, PetInteraction> all() {
        return byId;
    }

    static void replaceAll(Map<ResourceLocation, PetInteraction> interactions) {
        byId = Collections.unmodifiableSortedMap(new TreeMap<>(interactions));
    }
}
