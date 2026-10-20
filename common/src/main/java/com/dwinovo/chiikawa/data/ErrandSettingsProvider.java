package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettingsLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link ErrandSettingsData} to {@code data/chiikawa/pet_errand/<errand>.json}.
 * Shared by both loaders' data generators.
 */
public final class ErrandSettingsProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public ErrandSettingsProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, ErrandSettingsLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(ErrandSettingsData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                ErrandSettings.codecOf(entry.getKey()).encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Errand Settings";
    }
}
