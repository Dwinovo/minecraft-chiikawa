package com.dwinovo.chiikawa.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class FabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        // 1.20.1: Use explicit Factory cast to avoid ambiguous method call
        for (String locale : LanguageData.LOCALES) {
            pack.addProvider((FabricDataGenerator.Pack.Factory<FabricModLanguageProvider>) output -> new FabricModLanguageProvider(output, locale));
        }
        pack.addProvider(FabricModItemModelProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<ModBlockModelProvider>) ModBlockModelProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<PropItemModelProvider>) PropItemModelProvider::new);
        pack.addProvider(FabricModBlockTagsProvider::new);
        pack.addProvider(FabricModItemTagsProvider::new);
        pack.addProvider(FabricModEntityTagsProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<FabricModSoundsProvider>) FabricModSoundsProvider::new);
        pack.addProvider(FabricModRecipeProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<PetPersonalityProvider>) PetPersonalityProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<PetVoiceProvider>) PetVoiceProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<PetTaskTypeProvider>) PetTaskTypeProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<GameTestStructureProvider>) GameTestStructureProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<ShopCatalogProvider>) ShopCatalogProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<LaborBoardLevelProvider>) LaborBoardLevelProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<PetSpawnProvider>) PetSpawnProvider::new);
        pack.addProvider((net.minecraft.data.DataProvider.Factory<ManualProvider>) ManualProvider::new);
        pack.addProvider((output, registries) -> new ModAdvancementProvider(output, registries));
        pack.addProvider((net.minecraft.data.DataProvider.Factory<ModLootTableProvider>) ModLootTableProvider::new);
    }
}
