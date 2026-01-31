package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ForgeModItemModelProvider extends ItemModelProvider {

    public ForgeModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Constants.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        generateSpawnEggs();
        // Weapons have prebuilt models under resources; no datagen needed.
    }

    private void generateSpawnEggs() {
        spawnEggItem(InitItems.USAGI_SPAWN_EGG.get());
        spawnEggItem(InitItems.HACHIWARE_SPAWN_EGG.get());
        spawnEggItem(InitItems.CHIIKAWA_SPAWN_EGG.get());
        spawnEggItem(InitItems.SHISA_SPAWN_EGG.get());
        spawnEggItem(InitItems.MOMONGA_SPAWN_EGG.get());
        spawnEggItem(InitItems.KURIMANJU_SPAWN_EGG.get());
        spawnEggItem(InitItems.RAKKO_SPAWN_EGG.get());
    }

    private void spawnEggItem(net.minecraft.world.item.Item item) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        getBuilder(id.getPath())
            .parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile("item/generated"))
            .texture("layer0", new ResourceLocation(id.getNamespace(), "item/" + id.getPath()));
    }
}
