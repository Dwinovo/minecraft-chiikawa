package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;

import com.dwinovo.chiikawa.data.TagData;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
public class ModItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider,
            item -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addItemTags(new TagData.TagAppenderProvider<>() {
            @Override
            public void add(TagKey<Item> key, Item... values) {
                tag(key).add(values);
            }

            @Override
            public void addOptionalTag(TagKey<Item> key, ResourceLocation includedTagId) {
                tag(key).addOptionalTag(includedTagId);
            }
        });
    }
}
