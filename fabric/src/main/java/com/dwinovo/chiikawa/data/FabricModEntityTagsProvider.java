package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;

public class FabricModEntityTagsProvider extends FabricTagProvider.EntityTypeTagProvider {
    public FabricModEntityTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addEntityTags(this::valueLookupBuilder);
    }
}
