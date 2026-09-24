package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitSounds;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ForgeModSoundsProvider implements DataProvider {
    private final PackOutput output;
    private final Map<String, List<ResourceLocation>> variants;

    public ForgeModSoundsProvider(PackOutput output) {
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
                // A sound with nothing but its name is written as the name alone, the short
                // form vanilla's and NeoForge's providers write, so sounds.json reads the same.
                .forEach(sound -> soundList.add(sound.toString()));
            JsonObject definition = new JsonObject();
            definition.add("sounds", soundList);
            root.add(entry.path(), definition);
        }
        PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "");
        return DataProvider.saveStable(cache, root,
            pathProvider.json(new ResourceLocation(Constants.MOD_ID, "sounds")));
    }

    @Override
    public String getName() {
        return "Chiikawa Sounds";
    }
}
