package com.dwinovo.chiikawa.qualification;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The licences currently loaded from data packs. Server-side only; replaced as a whole on
 * every data pack (re)load.
 */
public final class Qualifications {
    private static volatile SortedMap<ResourceLocation, Qualification> byId = Collections.emptySortedMap();

    private Qualifications() {
    }

    /** @return every loaded licence by id, in id order */
    public static SortedMap<ResourceLocation, Qualification> all() {
        return byId;
    }

    /** @return the licence with this id, if a data pack has it */
    public static Optional<Qualification> get(ResourceLocation id) {
        return Optional.ofNullable(byId.get(id));
    }

    static void replaceAll(Map<ResourceLocation, Qualification> qualifications) {
        byId = Collections.unmodifiableSortedMap(new TreeMap<>(qualifications));
    }
}
