package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
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
        TagData.addItemTags((key, values) -> tag(key).add(values));
    }
}
