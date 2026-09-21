package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;
import com.google.gson.JsonObject;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.world.item.Item;

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

        itemModelGenerator.generateFlatItem(InitItems.SIMPLE_DISH.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.PET_BELL.get(), ModelTemplates.FLAT_ITEM);

        // Weapons have prebuilt models under resources; no datagen needed.
        itemModelGenerator.generateFlatItem(InitItems.MUSIC_BOX.get(), ModelTemplates.FLAT_ITEM);

        // Bags are drawn from their own Bedrock models by a built-in renderer, lit from
        // the front in a slot the way a flat item is.
        for (Supplier<Item> bag : InitItems.BAGS) {
            itemModelGenerator.output.accept(ModelLocationUtils.getModelLocation(bag.get()), () -> {
                JsonObject model = new JsonObject();
                model.addProperty("parent", "minecraft:builtin/entity");
                model.addProperty("gui_light", "front");
                return model;
            });
        }
    }

    private static void generateSpawnEggs(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(InitItems.USAGI_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.HACHIWARE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.CHIIKAWA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.SHISA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.MOMONGA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.KURIMANJU_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.RAKKO_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.FURUHONYA_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
    }

    private static void generateDolls(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(InitItems.USAGI_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.HACHIWARE_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.CHIIKAWA_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.SHISA_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.MOMONGA_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.KURIMANJU_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.RAKKO_DOLL.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(InitItems.FURUHONYA_DOLL.get(), ModelTemplates.FLAT_ITEM);
    }
}
