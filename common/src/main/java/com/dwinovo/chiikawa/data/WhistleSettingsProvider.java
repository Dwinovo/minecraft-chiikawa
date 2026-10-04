package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.whistle.WhistleSettings;
import com.dwinovo.chiikawa.whistle.WhistleSettingsLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link WhistleSettingsData} to {@code data/chiikawa/pet_whistle/<item>.json}.
 * Shared by both loaders' data generators.
 */
public final class WhistleSettingsProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public WhistleSettingsProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, WhistleSettingsLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(WhistleSettingsData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                WhistleSettings.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Whistle Settings";
    }
}
