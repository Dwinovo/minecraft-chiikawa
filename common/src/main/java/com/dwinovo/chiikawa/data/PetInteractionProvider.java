package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.PetInteractionLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link PetInteractionData} to {@code data/chiikawa/pet_interaction/<id>.json}.
 * Shared by both loaders' data generators.
 */
public final class PetInteractionProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public PetInteractionProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, PetInteractionLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(PetInteractionData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                PetInteraction.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Constants.LOG::error),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Pet Interactions";
    }
}
