package com.dwinovo.chiikawa.data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.SoundData;
import com.dwinovo.chiikawa.init.InitSounds;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public final class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    private final Map<String, List<Identifier>> variants;

    public ModSoundDefinitionsProvider(PackOutput output) {
        super(output, Constants.MOD_ID);
        this.variants = SoundData.collectVariants(output);
    }

    @Override
    public void registerSounds() {
        for (InitSounds.SoundEntry entry : InitSounds.entries()) {
            List<Identifier> sounds = variants.get(entry.path());
            if (sounds == null || sounds.isEmpty()) {
                continue;
            }
            SoundDefinition.Sound[] soundEntries = sounds.stream()
                    .sorted(Comparator.comparing(Identifier::toString))
                    .map(SoundDefinitionsProvider::sound)
                    .toArray(SoundDefinition.Sound[]::new);
            add(entry.holder().get(), SoundDefinition.definition().with(soundEntries));
        }
    }
}


