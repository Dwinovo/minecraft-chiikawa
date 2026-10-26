package com.dwinovo.chiikawa.entity.brain.errand.remark;

import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The remarks currently loaded from data packs, by id. Server-side only; replaced as a whole on every data pack
 * (re)load.
 */
public final class PetRemarks {
    private static volatile Map<Identifier, PetRemark> byId = Map.of();

    private PetRemarks() {
    }

    /** @return every remark, by id */
    public static Map<Identifier, PetRemark> all() {
        return byId;
    }

    /** Replaces them all, as a data pack reload does. Public so an in-game case can try other remarks. */
    public static void replaceAll(Map<Identifier, PetRemark> remarks) {
        byId = Map.copyOf(remarks);
    }
}
