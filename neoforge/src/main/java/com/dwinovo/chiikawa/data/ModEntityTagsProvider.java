package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;

import com.dwinovo.chiikawa.Constants;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;

public class ModEntityTagsProvider extends EntityTypeTagsProvider{
    public ModEntityTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, Constants.MOD_ID);
    }
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addEntityTags(this::tag);
    }
}


