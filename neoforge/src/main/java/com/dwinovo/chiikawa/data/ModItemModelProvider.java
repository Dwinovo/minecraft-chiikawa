package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Constants.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(InitItems.USAGI_SPAWN_EGG.get());
        basicItem(InitItems.HACHIWARE_SPAWN_EGG.get());
        basicItem(InitItems.CHIIKAWA_SPAWN_EGG.get());
        basicItem(InitItems.SHISA_SPAWN_EGG.get());
        basicItem(InitItems.MOMONGA_SPAWN_EGG.get());
        basicItem(InitItems.KURIMANJU_SPAWN_EGG.get());
        basicItem(InitItems.RAKKO_SPAWN_EGG.get());
        basicItem(InitItems.FURUHONYA_SPAWN_EGG.get());

        basicItem(InitItems.USAGI_DOLL.get());
        basicItem(InitItems.HACHIWARE_DOLL.get());
        basicItem(InitItems.CHIIKAWA_DOLL.get());
        basicItem(InitItems.SHISA_DOLL.get());
        basicItem(InitItems.MOMONGA_DOLL.get());
        basicItem(InitItems.KURIMANJU_DOLL.get());
        basicItem(InitItems.RAKKO_DOLL.get());
        basicItem(InitItems.FURUHONYA_DOLL.get());

        // Weapons have prebuilt models under resources; no datagen needed.
        basicItem(InitItems.MUSIC_BOX.get());
        basicItem(InitItems.SIMPLE_DISH.get());
        basicItem(InitItems.PET_BELL.get());

        // Bags are drawn from their own Bedrock models by a built-in renderer, lit from
        // the front in a slot the way a flat item is.
        for (Supplier<Item> bag : InitItems.BAGS) {
            getBuilder(BuiltInRegistries.ITEM.getKey(bag.get()).getPath())
                .parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                .guiLight(BlockModel.GuiLight.FRONT);
        }
    }
}
