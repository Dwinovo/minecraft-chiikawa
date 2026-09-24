package com.dwinovo.chiikawa.shop;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/<namespace>/shop_catalog/<id>.json}. A file
 * that fails to parse is skipped with a message: a shop with a broken price list should
 * stand there selling nothing, not take the world down with it.
 */
public final class ShopCatalogLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "shop_catalog";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-shop] ";

    public ShopCatalogLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        ShopCatalogs.replaceAll(loaded.catalogs());
        Constants.LOG.info(LOG_PREFIX + "loaded {} shop catalogs", loaded.catalogs().size());
    }

    /**
     * @param files parsed JSON by file id
     * @return the price lists that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, ShopCatalog> catalogs = new HashMap<>();
        List<String> errors = new ArrayList<>();
        files.forEach((id, json) -> {
            DataResult<ShopCatalog> parsed = ShopCatalog.CODEC.parse(JsonOps.INSTANCE, json);
            parsed.result().ifPresent(catalog -> catalogs.put(id, catalog));
            parsed.error().ifPresent(error -> errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message()));
        });
        return new Loaded(catalogs, errors);
    }

    /**
     * @param catalogs decoded price lists by id
     * @param errors one message per file that could not be decoded
     */
    record Loaded(Map<ResourceLocation, ShopCatalog> catalogs, List<String> errors) {
    }
}
