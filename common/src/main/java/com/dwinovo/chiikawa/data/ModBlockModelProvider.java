package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.block.LaborBoardBlock;
import com.dwinovo.chiikawa.block.ShopBlock;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Block states and models of the mod's blocks, drawn from vanilla textures. Shared by
 * both loaders' data generators, since vanilla model generation has no way to describe
 * a model's own elements.
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
        LaborBoardBlock board = InitBlocks.LABOR_BOARD.get();
        ResourceLocation boardModel = ModelLocationUtils.getModelLocation(board);
        ShopBlock shop = InitBlocks.SHOP.get();
        ResourceLocation shopModel = ModelLocationUtils.getModelLocation(shop);
        return CompletableFuture.allOf(
            DataProvider.saveStable(cache, horizontalBlockState(board, LaborBoardBlock.FACING, boardModel),
                blockStates.json(BuiltInRegistries.BLOCK.getKey(board))),
            DataProvider.saveStable(cache, laborBoardModel(), models.json(boardModel)),
            DataProvider.saveStable(cache, new DelegatedModel(boardModel).get(), models.json(ModelLocationUtils.getModelLocation(board.asItem()))),
            DataProvider.saveStable(cache, horizontalBlockState(shop, ShopBlock.FACING, shopModel),
                blockStates.json(BuiltInRegistries.BLOCK.getKey(shop))),
            DataProvider.saveStable(cache, shopModel(), models.json(shopModel)),
            DataProvider.saveStable(cache, new DelegatedModel(shopModel).get(), models.json(ModelLocationUtils.getModelLocation(shop.asItem())))
        );
    }

    /** The counter: a panelled front, a worn top, and an awning post at each end. */
    private static JsonElement shopModel() {
        JsonObject textures = new JsonObject();
        textures.addProperty("front", TextureMapping.getBlockTexture(Blocks.SPRUCE_PLANKS).toString());
        textures.addProperty("top", TextureMapping.getBlockTexture(Blocks.STRIPPED_SPRUCE_LOG, "_top").toString());
        textures.addProperty("post", TextureMapping.getBlockTexture(Blocks.DARK_OAK_LOG).toString());
        textures.addProperty("particle", TextureMapping.getBlockTexture(Blocks.SPRUCE_PLANKS).toString());

        List<Direction> all = List.of(Direction.values());
        JsonArray elements = new JsonArray();
        // The body of the counter, and the top overhanging it towards the customer.
        elements.add(element(1, 0, 3, 15, 12, 13, "front", all));
        elements.add(element(0, 12, 0, 16, 14, 14, "top", all));
        // Corner posts, so it reads as a stall rather than a block of planks.
        elements.add(element(0, 0, 3, 1, 12, 4, "post", all));
        elements.add(element(15, 0, 3, 16, 12, 4, "post", all));
        return model(textures, elements);
    }

    /** The model faces north; the other directions turn it about the vertical axis. */
    private static JsonElement horizontalBlockState(Block block, DirectionProperty facing, ResourceLocation model) {
        return MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, model))
            .with(PropertyDispatch.property(facing)
                .select(Direction.NORTH, Variant.variant())
                .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)))
            .get();
    }

    /** Two dark posts holding a birch board, with three notes pinned on its north face. */
    private static JsonElement laborBoardModel() {
        JsonObject textures = new JsonObject();
        textures.addProperty("post", TextureMapping.getBlockTexture(Blocks.DARK_OAK_LOG).toString());
        textures.addProperty("board", TextureMapping.getBlockTexture(Blocks.BIRCH_PLANKS).toString());
        textures.addProperty("note", TextureMapping.getBlockTexture(Blocks.WHITE_WOOL).toString());
        textures.addProperty("particle", TextureMapping.getBlockTexture(Blocks.BIRCH_PLANKS).toString());

        List<Direction> all = List.of(Direction.values());
        // A note lies flat on the board, so its back face is never seen.
        List<Direction> note = List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);
        JsonArray elements = new JsonArray();
        elements.add(element(1, 0, 7, 3, 16, 9, "post", all));
        elements.add(element(13, 0, 7, 15, 16, 9, "post", all));
        elements.add(element(0, 6, 6, 16, 15, 7, "board", all));
        elements.add(element(2, 9, 5.5F, 6, 13.5F, 6, "note", note));
        elements.add(element(7, 7.5F, 5.5F, 10, 12, 6, "note", note));
        elements.add(element(11, 9.5F, 5.5F, 14, 13.5F, 6, "note", note));

        return model(textures, elements);
    }

    /** A model of nothing but its own boxes, so a block needs no art of its own. */
    private static JsonElement model(JsonObject textures, JsonArray elements) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", ResourceLocation.withDefaultNamespace("block/block").toString());
        model.add("textures", textures);
        model.add("elements", elements);
        return model;
    }

    private static JsonObject element(float fromX, float fromY, float fromZ, float toX, float toY, float toZ,
            String texture, List<Direction> faces) {
        JsonObject faceJson = new JsonObject();
        for (Direction face : faces) {
            JsonObject entry = new JsonObject();
            entry.addProperty("texture", "#" + texture);
            faceJson.add(face.getSerializedName(), entry);
        }
        JsonObject element = new JsonObject();
        element.add("from", vector(fromX, fromY, fromZ));
        element.add("to", vector(toX, toY, toZ));
        element.add("faces", faceJson);
        return element;
    }

    private static JsonArray vector(float x, float y, float z) {
        JsonArray vector = new JsonArray();
        vector.add(x);
        vector.add(y);
        vector.add(z);
        return vector;
    }

    @Override
    public String getName() {
        return "Chiikawa Block Models";
    }
}
