package com.dwinovo.chiikawa.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class FabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        // 1.20.1: Use explicit Factory cast to avoid ambiguous method call
        pack.addProvider((FabricDataGenerator.Pack.Factory<FabricModLanguageProvider>) output -> new FabricModLanguageProvider(output, "en_us"));
        pack.addProvider((FabricDataGenerator.Pack.Factory<FabricModLanguageProvider>) output -> new FabricModLanguageProvider(output, "zh_cn"));
        pack.addProvider(FabricModItemModelProvider::new);
        pack.addProvider(FabricModBlockTagsProvider::new);
        pack.addProvider(FabricModItemTagsProvider::new);
        pack.addProvider(FabricModEntityTagsProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<FabricModSoundsProvider>) FabricModSoundsProvider::new);
        pack.addProvider(FabricModRecipeProvider::new);
    }
}
