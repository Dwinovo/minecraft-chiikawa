package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.data.TagData;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class FabricModItemTagsProvider extends FabricTagProvider.ItemTagProvider {
    public FabricModItemTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addItemTags(new TagData.TagAppenderProvider<>() {
            @Override
            public void add(TagKey<Item> key, Item... values) {
                getOrCreateTagBuilder(key).add(values);
            }

            @Override
            public void addOptionalTag(TagKey<Item> key, ResourceLocation includedTagId) {
                getOrCreateTagBuilder(key).addOptionalTag(includedTagId);
            }
        });
    }
}
