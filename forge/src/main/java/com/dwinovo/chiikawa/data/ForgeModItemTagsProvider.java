package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import javax.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

public class ForgeModItemTagsProvider extends ItemTagsProvider {

    public ForgeModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, 
                                     CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Constants.MOD_ID, existingFileHelper);
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
