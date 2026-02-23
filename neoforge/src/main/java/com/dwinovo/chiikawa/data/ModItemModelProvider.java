package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
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

        basicItem(InitItems.USAGI_DOLL.get());
        basicItem(InitItems.HACHIWARE_DOLL.get());
        basicItem(InitItems.CHIIKAWA_DOLL.get());
    }
}
