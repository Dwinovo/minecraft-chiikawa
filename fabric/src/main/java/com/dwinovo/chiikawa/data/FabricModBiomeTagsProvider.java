package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class FabricModBiomeTagsProvider extends FabricTagProvider<Biome> {
    public FabricModBiomeTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.BIOME, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagData.addBiomeTags(new TagData.BiomeTagAppender() {
            @Override
            public void add(TagKey<Biome> key, ResourceKey<Biome>... biomes) {
                for (ResourceKey<Biome> biome : biomes) {
                    getOrCreateTagBuilder(key).add(biome);
                }
            }

            @Override
            public void addOptionalTag(TagKey<Biome> key, ResourceLocation includedTagId) {
                getOrCreateTagBuilder(key).addOptionalTag(includedTagId);
            }
        });
    }
}
