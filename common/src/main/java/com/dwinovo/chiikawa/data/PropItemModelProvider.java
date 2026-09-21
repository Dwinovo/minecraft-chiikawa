package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * The item models of the props ({@link InitItems#PROPS}): each only says "drawn by a
 * built-in renderer", which hands it to {@code PropRenderer} and the prop's own Bedrock
 * model. Shared by both loaders' data generators, so the list and the rule live once.
 *
 * <p>A block's item is lit from the side in a slot, as vanilla lights a block; anything
 * else from the front, as vanilla lights a flat item.
 */
public final class PropItemModelProvider implements DataProvider {
    private final PackOutput.PathProvider models;

    public PropItemModelProvider(PackOutput output) {
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
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
        model.addProperty("parent", "minecraft:builtin/entity");
        if (!(item instanceof BlockItem)) {
            model.addProperty("gui_light", "front");
        }
        return model;
    }

    @Override
    public String getName() {
        return "Chiikawa Prop Item Models";
    }
}
