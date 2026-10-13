package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ForgeModBiomeTagsProvider extends BiomeTagsProvider {

    public ForgeModBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Constants.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addBiomeTags(new TagData.BiomeTagAppender() {
            @Override
            public void add(TagKey<Biome> key, ResourceKey<Biome>... biomes) {
                tag(key).add(biomes);
            }

            @Override
            public void addOptionalTag(TagKey<Biome> key, ResourceLocation includedTagId) {
                tag(key).addOptionalTag(includedTagId);
            }
        });
    }
}
