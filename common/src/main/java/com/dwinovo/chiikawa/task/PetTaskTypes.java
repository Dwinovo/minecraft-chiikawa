package com.dwinovo.chiikawa.task;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/**
 * The task types currently loaded from data packs. Server-side only; replaced as a
 * whole on every data pack (re)load.
 */
public final class PetTaskTypes {
    private static volatile SortedMap<Identifier, PetTaskType> byId = Collections.emptySortedMap();

    private PetTaskTypes() {
    }

    /** @return every loaded type by id, in id order so a board's roll does not depend on load order */
    public static SortedMap<Identifier, PetTaskType> all() {
        return byId;
    }

    static void replaceAll(Map<Identifier, PetTaskType> types) {
        byId = Collections.unmodifiableSortedMap(new TreeMap<>(types));
    }
}
