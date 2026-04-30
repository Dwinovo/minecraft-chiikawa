package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.data.TagData;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;

public class FabricModItemTagsProvider extends FabricTagsProvider.ItemTagsProvider {
    public FabricModItemTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addItemTags(this::valueLookupBuilder);
    }
}
