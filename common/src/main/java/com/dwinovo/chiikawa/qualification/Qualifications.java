package com.dwinovo.chiikawa.qualification;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/**
 * The licences currently loaded from data packs. Server-side only; replaced as a whole on
 * every data pack (re)load.
 */
public final class Qualifications {
    private static volatile SortedMap<Identifier, Qualification> byId = Collections.emptySortedMap();

    private Qualifications() {
    }

    /** @return every loaded licence by id, in id order */
    public static SortedMap<Identifier, Qualification> all() {
        return byId;
    }

    /** @return the licence with this id, if a data pack has it */
    public static Optional<Qualification> get(Identifier id) {
        return Optional.ofNullable(byId.get(id));
    }

    static void replaceAll(Map<Identifier, Qualification> qualifications) {
        byId = Collections.unmodifiableSortedMap(new TreeMap<>(qualifications));
    }
}
