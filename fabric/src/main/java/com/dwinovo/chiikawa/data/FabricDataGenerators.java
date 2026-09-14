package com.dwinovo.chiikawa.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class FabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider((output, registries) -> new FabricModLanguageProvider(output, "en_us", registries));
        pack.addProvider((output, registries) -> new FabricModLanguageProvider(output, "zh_cn", registries));
        pack.addProvider(FabricModItemModelProvider::new);
        pack.addProvider(FabricModBlockTagsProvider::new);
        pack.addProvider(FabricModItemTagsProvider::new);
        pack.addProvider(FabricModEntityTagsProvider::new);
        pack.addProvider(FabricModSoundsProvider::new);
        pack.addProvider(ModRecipeProvider.Runner::new);
    }
}
