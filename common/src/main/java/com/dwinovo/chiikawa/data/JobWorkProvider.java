package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.job.JobWork;
import com.dwinovo.chiikawa.entity.job.JobWorkLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link JobWorkData} to {@code data/chiikawa/pet_job/<job>.json}.
 * Shared by both loaders' data generators.
 */
public final class JobWorkProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public JobWorkProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, JobWorkLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(JobWorkData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                JobWork.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Job Work";
    }
}
