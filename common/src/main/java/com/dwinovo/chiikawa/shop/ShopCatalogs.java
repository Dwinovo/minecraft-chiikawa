package com.dwinovo.chiikawa.shop;

import java.util.Collections;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The price lists currently loaded from data packs. Server-side only; replaced as a whole
 * on every data pack (re)load, so a shop always quotes what the pack says today.
 */
public final class ShopCatalogs {
    private static volatile Map<ResourceLocation, ShopCatalog> byId = Collections.emptyMap();

    private ShopCatalogs() {
    }

    /** @return every loaded price list by id */
    public static Map<ResourceLocation, ShopCatalog> all() {
        return byId;
    }

    /**
     * @param id the price list a shop block names
     * @return that list, or an empty one — a shop whose list went missing deals in nothing
     *         rather than throwing every time a pet walks past it
     */
    public static ShopCatalog get(ResourceLocation id) {
        return byId.getOrDefault(id, ShopCatalog.EMPTY);
    }

    static void replaceAll(Map<ResourceLocation, ShopCatalog> catalogs) {
        byId = Map.copyOf(catalogs);
    }
}
