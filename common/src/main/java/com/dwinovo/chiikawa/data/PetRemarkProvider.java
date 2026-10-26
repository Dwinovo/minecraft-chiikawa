package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.errand.remark.PetRemark;
import com.dwinovo.chiikawa.entity.brain.errand.remark.PetRemarkLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link PetRemarkData} to {@code data/chiikawa/pet_remark/<id>.json}.
 * Shared by both loaders' data generators.
 */
public final class PetRemarkProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public PetRemarkProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, PetRemarkLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(PetRemarkData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                PetRemark.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Pet Remarks";
    }
}
