package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitBlocks;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Block states and models of the mod's blocks. The labor board and the shop are drawn from
 * their Bedrock models by their block entities' renderers, so their block models are only
 * there for their break particles, and their item models come with the other props' from
 * {@link PropItemModelProvider}. Shared by both loaders' data generators.
 */
public final class ModBlockModelProvider implements DataProvider {
    private final PackOutput.PathProvider blockStates;
    private final PackOutput.PathProvider models;

    public ModBlockModelProvider(PackOutput output) {
        this.blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(
            drawnByItsRenderer(cache, InitBlocks.LABOR_BOARD.get(), Blocks.SPRUCE_PLANKS),
            drawnByItsRenderer(cache, InitBlocks.SHOP.get(), Blocks.BIRCH_PLANKS)
        );
    }

    /**
     * A block drawn by its block entity's renderer, as vanilla's chest is: one block model
     * for every state, with nothing for it to draw, only a texture for the bits that fly off
     * the block when it breaks. Which way the block faces is the renderer's business.
     */
    private CompletableFuture<?> drawnByItsRenderer(CachedOutput cache, Block block, Block like) {
        ResourceLocation model = ModelLocationUtils.getModelLocation(block);
        return CompletableFuture.allOf(
            DataProvider.saveStable(cache,
                MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, model)).get(),
                blockStates.json(BuiltInRegistries.BLOCK.getKey(block))),
            DataProvider.saveStable(cache, particlesOnly(like), models.json(model))
        );
    }

    private static JsonElement particlesOnly(Block like) {
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", TextureMapping.getBlockTexture(like).toString());
        JsonObject model = new JsonObject();
        model.add("textures", textures);
        return model;
    }

    @Override
    public String getName() {
        return "Chiikawa Block Models";
    }
}
