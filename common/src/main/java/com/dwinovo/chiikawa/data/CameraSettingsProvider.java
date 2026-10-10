package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.CameraSettingsLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link CameraSettingsData} to {@code data/chiikawa/pet_camera/<item>.json}.
 * Shared by both loaders' data generators.
 */
public final class CameraSettingsProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public CameraSettingsProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, CameraSettingsLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(CameraSettingsData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                CameraSettings.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Camera Settings";
    }
}
