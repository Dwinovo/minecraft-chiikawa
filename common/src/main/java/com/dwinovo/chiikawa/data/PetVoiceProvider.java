package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.voice.PetVoice;
import com.dwinovo.chiikawa.voice.PetVoiceLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link PetVoiceData} to {@code data/chiikawa/pet_voice/<entity>.json}.
 * Shared by both loaders' data generators.
 */
public final class PetVoiceProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public PetVoiceProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, PetVoiceLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(PetVoiceData.all().entrySet().stream()
            .map(entry -> DataProvider.saveStable(cache,
                PetVoice.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow(false, Constants.LOG::error),
                pathProvider.json(entry.getKey())))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chiikawa Pet Voices";
    }
}
