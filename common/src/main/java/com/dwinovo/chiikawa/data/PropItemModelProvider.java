package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.client.render.PropItemRenderer;
import com.dwinovo.chiikawa.init.InitItems;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * The item models of the props ({@link InitItems#PROPS}): each only says "drawn by a
 * special renderer", which hands it to {@code PropRenderer} and the prop's own Bedrock
 * model. Shared by both loaders' data generators, so the list and the rule live once.
 *
 * <p>A block's item is lit from the side in a slot, as vanilla lights a block; anything
 * else from the front, as vanilla lights a flat item. A block's item model is its block's,
 * for the texture of the bits that fly off it.
 */
public final class PropItemModelProvider implements DataProvider {
    private final PackOutput.PathProvider models;

    public PropItemModelProvider(PackOutput output) {
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    /**
     * The props' item definitions, handed to each loader's model provider, which writes every
     * item's definition: the special renderer, over the item model this provider writes.
     */
    public static void declareItems(ItemModelOutput output) {
        InitItems.PROPS.stream()
            .map(Supplier::get)
            .forEach(item -> output.accept(item,
                ItemModelUtils.specialModel(ModelLocationUtils.getModelLocation(item), new PropItemRenderer.Unbaked())));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(InitItems.PROPS.stream()
            .map(Supplier::get)
            .map(item -> DataProvider.saveStable(cache, model(item), models.json(ModelLocationUtils.getModelLocation(item))))
            .toArray(CompletableFuture[]::new));
    }

    private static JsonObject model(Item item) {
        JsonObject model = new JsonObject();
        if (item instanceof BlockItem block) {
            model.addProperty("parent", ModelLocationUtils.getModelLocation(block.getBlock()).toString());
        } else {
            model.addProperty("gui_light", "front");
        }
        return model;
    }

    @Override
    public String getName() {
        return "Chiikawa Prop Item Models";
    }
}
