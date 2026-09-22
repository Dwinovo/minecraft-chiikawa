package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.spawn.PetSpawn;
import com.dwinovo.chiikawa.spawn.PetSpawnLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link PetSpawnData} to {@code data/chiikawa/pet_spawn/<id>.json}. Shared by both
 * loaders' data generators, which read the same files back.
 */
public final class PetSpawnProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public PetSpawnProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, PetSpawnLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(PetSpawnData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                PetSpawn.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Pet Spawns";
    }
}
