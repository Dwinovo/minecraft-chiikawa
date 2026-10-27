package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.sensor.SurroundingsSettings;
import com.dwinovo.chiikawa.entity.brain.sensor.SurroundingsSettingsLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link SurroundingsSettingsData} to {@code data/chiikawa/pet_surroundings/surroundings.json}.
 * Shared by both loaders' data generators.
 */
public final class SurroundingsSettingsProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public SurroundingsSettingsProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, SurroundingsSettingsLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(SurroundingsSettingsData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                SurroundingsSettings.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Surroundings Settings";
    }
}
