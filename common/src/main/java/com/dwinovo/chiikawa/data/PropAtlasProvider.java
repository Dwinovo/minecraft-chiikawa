package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.AtlasIds;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;

/**
 * The textures of the props that wear out, one sprite each, on the block atlas, where
 * particles are drawn from: the way vanilla's own atlas definitions put its entity textures
 * there ({@code minecraft:atlases/blocks.json}, which every pack adds to). Their item models
 * name them as particle; see {@link PropItemModelProvider}. Shared by both loaders' data
 * generators.
 */
public final class PropAtlasProvider implements DataProvider {
    private final PackOutput.PathProvider atlases;

    public PropAtlasProvider(PackOutput output) {
        this.atlases = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "atlases");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<SpriteSource> sources = InitItems.PROPS.stream()
            .<Item>map(Supplier::get)
            .filter(PropItemModelProvider::wearsOut)
            .<SpriteSource>map(item -> new SingleFile(PropItemModelProvider.texture(item)))
            .toList();
        return DataProvider.saveStable(cache, SpriteSources.FILE_CODEC, sources, atlases.json(AtlasIds.BLOCKS));
    }

    @Override
    public String getName() {
        return "Chiikawa Prop Atlas";
    }
}
