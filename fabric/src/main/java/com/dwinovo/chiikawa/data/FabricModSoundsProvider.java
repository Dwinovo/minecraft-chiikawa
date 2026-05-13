package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.data.SoundData;
import com.dwinovo.chiikawa.init.InitSounds;
import com.dwinovo.chiikawa.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public class FabricModSoundsProvider implements DataProvider {
    private final PackOutput output;
    private final Map<String, List<ResourceLocation>> variants;

    public FabricModSoundsProvider(PackOutput output) {
        this.output = output;
        this.variants = SoundData.collectVariants(output);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject root = new JsonObject();
        for (InitSounds.SoundEntry entry : InitSounds.entries()) {
            List<ResourceLocation> sounds = variants.get(entry.path());
            if (sounds == null || sounds.isEmpty()) {
                continue;
            }
            JsonArray soundList = new JsonArray();
            sounds.stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(sound -> {
                    JsonObject soundEntry = new JsonObject();
                    soundEntry.addProperty("name", sound.toString());
                    addSoundMetadata(soundEntry, entry);
                    soundList.add(soundEntry);
                });
            JsonObject definition = new JsonObject();
            definition.add("sounds", soundList);
            root.add(entry.path(), definition);
        }
        PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "");
        return DataProvider.saveStable(cache, root,
            pathProvider.json(new ResourceLocation(Constants.MOD_ID, "sounds")));
    }

    private static void addSoundMetadata(JsonObject soundEntry, InitSounds.SoundEntry entry) {
        if (entry.stream()) {
            soundEntry.addProperty("stream", true);
        }
        if (entry.preload()) {
            soundEntry.addProperty("preload", true);
        }
        if (entry.attenuationDistance() != InitSounds.DEFAULT_ATTENUATION_DISTANCE) {
            soundEntry.addProperty("attenuation_distance", entry.attenuationDistance());
        }
    }

    @Override
    public String getName() {
        return "Chiikawa Sounds";
    }
}
