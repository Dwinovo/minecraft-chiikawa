package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.data.SoundData;
import com.dwinovo.chiikawa.init.InitSounds;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricSoundsProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

public class FabricModSoundsProvider extends FabricSoundsProvider {
    private final Map<String, List<Identifier>> variants;

    public FabricModSoundsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
        this.variants = SoundData.collectVariants(output);
    }

    @Override
    protected void configure(HolderLookup.Provider registryLookup, SoundExporter exporter) {
        for (InitSounds.SoundEntry entry : InitSounds.entries()) {
            List<Identifier> sounds = variants.get(entry.path());
            if (sounds == null || sounds.isEmpty()) {
                continue;
            }
            SoundTypeBuilder builder = SoundTypeBuilder.of();
            sounds.stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .map(SoundTypeBuilder.RegistrationBuilder::ofFile)
                .forEach(builder::sound);
            exporter.add(entry.holder().get(), builder);
        }
    }

    @Override
    public String getName() {
        return "Chiikawa Sounds";
    }
}
