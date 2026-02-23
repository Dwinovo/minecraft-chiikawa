package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public class FabricModItemModelProvider extends FabricModelProvider {
    public FabricModItemModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        generateSpawnEggs(itemModelGenerator);
        generateDolls(itemModelGenerator);

        // Weapons use custom Blockbench models under resources.
        itemModelGenerator.declareCustomModelItem(InitItems.USAGI_WEAPON.get());
        itemModelGenerator.declareCustomModelItem(InitItems.HACHIWARE_WEAPON.get());
        itemModelGenerator.declareCustomModelItem(InitItems.CHIIKAWA_WEAPON.get());
    }

    private static void generateSpawnEggs(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(InitItems.USAGI_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.HACHIWARE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.CHIIKAWA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.SHISA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.MOMONGA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.KURIMANJU_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.RAKKO_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
    }

    private static void generateDolls(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(InitItems.USAGI_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.HACHIWARE_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.CHIIKAWA_DOLL.get(), ModelTemplates.FLAT_ITEM);
    }
}
