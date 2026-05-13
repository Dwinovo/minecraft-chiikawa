package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.data.SoundData;
import com.dwinovo.chiikawa.init.InitSounds;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricSoundsProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

public class FabricModSoundsProvider extends FabricSoundsProvider {
    private final Map<String, List<ResourceLocation>> variants;

    public FabricModSoundsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
        this.variants = SoundData.collectVariants(output);
    }

    @Override
    protected void configure(HolderLookup.Provider registryLookup, SoundExporter exporter) {
        for (InitSounds.SoundEntry entry : InitSounds.entries()) {
            List<ResourceLocation> sounds = variants.get(entry.path());
            if (sounds == null || sounds.isEmpty()) {
                continue;
            }
            SoundTypeBuilder builder = SoundTypeBuilder.of();
            sounds.stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .map(sound -> sound(sound, entry))
                .forEach(builder::sound);
            exporter.add(entry.holder().get(), builder);
        }
    }

    private static SoundTypeBuilder.EntryBuilder sound(ResourceLocation sound, InitSounds.SoundEntry entry) {
        SoundTypeBuilder.EntryBuilder builder = SoundTypeBuilder.EntryBuilder.ofFile(sound);
        if (entry.stream()) {
            builder.stream(true);
        }
        if (entry.preload()) {
            builder.preload(true);
        }
        if (entry.attenuationDistance() != InitSounds.DEFAULT_ATTENUATION_DISTANCE) {
            builder.attenuationDistance(entry.attenuationDistance());
        }
        return builder;
    }

    @Override
    public String getName() {
        return "Chiikawa Sounds";
    }
}
