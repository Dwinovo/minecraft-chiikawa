package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.task.PetTaskType;
import com.dwinovo.chiikawa.task.PetTaskTypeLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link PetTaskTypeData} to {@code data/chiikawa/pet_task/<id>.json}.
 * Shared by both loaders' data generators.
 */
public final class PetTaskTypeProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public PetTaskTypeProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, PetTaskTypeLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(PetTaskTypeData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                PetTaskType.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Constants.LOG::error),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Pet Task Types";
    }
}
