package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;

import com.dwinovo.chiikawa.Constants;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public class ModBlockTagsProvider extends BlockTagsProvider{
    
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, Constants.MOD_ID);
    }
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addBlockTags((key, values) -> tag(key).add(values));
    }
}
