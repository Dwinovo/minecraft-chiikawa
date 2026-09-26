package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.qualification.Qualification;
import com.dwinovo.chiikawa.qualification.QualificationLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link QualificationData} to {@code data/chiikawa/pet_qualification/<id>.json}.
 * Shared by both loaders' data generators.
 */
public final class QualificationProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public QualificationProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, QualificationLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(QualificationData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                Qualification.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Pet Licences";
    }
}
