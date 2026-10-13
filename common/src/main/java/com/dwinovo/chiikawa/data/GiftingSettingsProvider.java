package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.gifting.GiftingSettings;
import com.dwinovo.chiikawa.gifting.GiftingSettingsLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link GiftingSettingsData} to {@code data/chiikawa/pet_gifting/gifting.json}.
 * Shared by both loaders' data generators.
 */
public final class GiftingSettingsProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public GiftingSettingsProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, GiftingSettingsLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(GiftingSettingsData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                GiftingSettings.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Constants.LOG::error),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Gifting Settings";
    }
}
