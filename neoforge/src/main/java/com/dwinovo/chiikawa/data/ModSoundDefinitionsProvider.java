package com.dwinovo.chiikawa.data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.SoundData;
import com.dwinovo.chiikawa.init.InitSounds;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    private final PackOutput output;

    public ModSoundDefinitionsProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Constants.MOD_ID, existingFileHelper);
        this.output = output;
    }

    @Override
    public void registerSounds() {
        Map<String, List<ResourceLocation>> variants = SoundData.collectVariants(output);
        for (InitSounds.SoundEntry entry : InitSounds.entries()) {
            List<ResourceLocation> sounds = variants.get(entry.path());
            if (sounds == null || sounds.isEmpty()) {
                continue;
            }
            SoundDefinition.Sound[] soundEntries = sounds.stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .map(sound -> sound(sound, entry))
                    .toArray(SoundDefinition.Sound[]::new);
            add(entry.holder().get(), SoundDefinition.definition().with(soundEntries));
        }
    }

    private static SoundDefinition.Sound sound(ResourceLocation sound, InitSounds.SoundEntry entry) {
        SoundDefinition.Sound definition = SoundDefinitionsProvider.sound(sound);
        if (entry.stream()) {
            definition.stream();
        }
        if (entry.preload()) {
            definition.preload();
        }
        if (entry.attenuationDistance() != InitSounds.DEFAULT_ATTENUATION_DISTANCE) {
            definition.attenuationDistance(entry.attenuationDistance());
        }
        return definition;
    }
}
