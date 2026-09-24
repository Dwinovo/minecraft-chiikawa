package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.manual.ManualLoader;
import com.dwinovo.chiikawa.manual.ManualPage;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link ManualData} to {@code assets/chiikawa/manual/<id>.json}. Shared by both
 * loaders' data generators.
 */
public final class ManualProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public ManualProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, ManualLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(ManualData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                ManualPage.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Constants.LOG::error),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Handbook Pages";
    }
}
