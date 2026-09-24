package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Chiikawa;
import com.dwinovo.chiikawa.init.InitItems;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

public final class ModItemModelProvider extends ModelProvider {
    public ModItemModelProvider(PackOutput output) {
        super(output, Chiikawa.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        generateSpawnEggs(itemModels);
        generateDolls(itemModels);

        itemModels.generateFlatItem(InitItems.MUSIC_BOX.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.SIMPLE_DISH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.PET_BELL.get(), ModelTemplates.FLAT_ITEM);
        // The props' item models, the weapons' among them, come from PropItemModelProvider, shared with Fabric.
        PropItemModelProvider.generate(itemModels.itemModelOutput, itemModels.modelOutput);
        // The blocks' states and models are shared with Fabric.
        ModBlockModelProvider.generate(blockModels);
    }

    private static void generateSpawnEggs(ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(InitItems.USAGI_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.HACHIWARE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.CHIIKAWA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.SHISA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.MOMONGA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.KURIMANJU_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.RAKKO_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.FURUHONYA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
    }

    private static void generateDolls(ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(InitItems.USAGI_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.HACHIWARE_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.CHIIKAWA_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.SHISA_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.MOMONGA_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.KURIMANJU_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.RAKKO_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(InitItems.FURUHONYA_DOLL.get(), ModelTemplates.FLAT_ITEM);
    }
}
