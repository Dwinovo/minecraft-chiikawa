package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ForgeModItemModelProvider extends ItemModelProvider {

    public ForgeModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Constants.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        generateSpawnEggs();
        generateDolls();
        generatedItem(InitItems.MUSIC_BOX.get());
        generatedItem(InitItems.SIMPLE_DISH.get());
        generatedItem(InitItems.PET_BELL.get());

        // Bags are drawn from their own Bedrock models by a built-in renderer, lit from
        // the front in a slot the way a flat item is.
        for (Supplier<Item> bag : InitItems.BAGS) {
            getBuilder(BuiltInRegistries.ITEM.getKey(bag.get()).getPath())
                .parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                .guiLight(BlockModel.GuiLight.FRONT);
        }
        // Weapons have prebuilt models under resources; no datagen needed.
    }

    private void generateSpawnEggs() {
        generatedItem(InitItems.USAGI_SPAWN_EGG.get());
        generatedItem(InitItems.HACHIWARE_SPAWN_EGG.get());
        generatedItem(InitItems.CHIIKAWA_SPAWN_EGG.get());
        generatedItem(InitItems.SHISA_SPAWN_EGG.get());
        generatedItem(InitItems.MOMONGA_SPAWN_EGG.get());
        generatedItem(InitItems.KURIMANJU_SPAWN_EGG.get());
        generatedItem(InitItems.RAKKO_SPAWN_EGG.get());
        generatedItem(InitItems.FURUHONYA_SPAWN_EGG.get());
    }

    private void generateDolls() {
        generatedItem(InitItems.USAGI_DOLL.get());
        generatedItem(InitItems.HACHIWARE_DOLL.get());
        generatedItem(InitItems.CHIIKAWA_DOLL.get());
        generatedItem(InitItems.SHISA_DOLL.get());
        generatedItem(InitItems.MOMONGA_DOLL.get());
        generatedItem(InitItems.KURIMANJU_DOLL.get());
        generatedItem(InitItems.RAKKO_DOLL.get());
        generatedItem(InitItems.FURUHONYA_DOLL.get());
    }

    private void generatedItem(net.minecraft.world.item.Item item) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        getBuilder(id.getPath())
            .parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile("item/generated"))
            .texture("layer0", new ResourceLocation(id.getNamespace(), "item/" + id.getPath()));
    }
}
