package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalityLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link PersonalityData} to {@code data/chiikawa/pet_personality/<entity>.json}.
 * Shared by both loaders' data generators.
 */
public final class PetPersonalityProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public PetPersonalityProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, PetPersonalityLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(PersonalityData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                Personality.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Constants.LOG::error),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Pet Personalities";
    }
}
