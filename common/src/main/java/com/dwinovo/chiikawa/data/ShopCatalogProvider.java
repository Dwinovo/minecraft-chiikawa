package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.shop.ShopCatalog;
import com.dwinovo.chiikawa.shop.ShopCatalogLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link ShopCatalogData} to {@code data/chiikawa/shop_catalog/<id>.json}.
 * Shared by both loaders' data generators.
 */
public final class ShopCatalogProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public ShopCatalogProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, ShopCatalogLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(ShopCatalogData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                ShopCatalog.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Constants.LOG::error),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Shop Catalogs";
    }
}
