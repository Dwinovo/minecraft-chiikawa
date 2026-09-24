package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.dwinovo.chiikawa.init.InitItems;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * The item models of the props ({@link InitItems#PROPS}): each only says "drawn by a
 * special item renderer", which hands it to {@code PropRenderer} and the prop's own
 * Bedrock model. Shared by both loaders' data generators, so the list and the rule live
 * once.
 *
 * <p>A block's item borrows vanilla's block transforms and is lit from the side in a slot,
 * as vanilla lights a block; anything else borrows a flat item's transforms and is lit
 * from the front, as vanilla lights a flat item. Neither has a picture of its own to crumble
 * into, which the particle says.
 */
public final class PropItemModelProvider implements DataProvider {
    private final PackOutput.PathProvider models;
    private final PackOutput.PathProvider items;

    public PropItemModelProvider(PackOutput output) {
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.items = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(InitItems.PROPS.stream()
            .map(Supplier::get)
            .map(item -> CompletableFuture.allOf(
                DataProvider.saveStable(cache, model(item), models.json(ModelLocationUtils.getModelLocation(item))),
                DataProvider.saveStable(cache, itemInfo(item), items.json(BuiltInRegistries.ITEM.getKey(item)))))
            .toArray(CompletableFuture[]::new));
    }

    private static JsonObject model(Item item) {
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:missingno");
        JsonObject model = new JsonObject();
        model.addProperty("parent", item instanceof BlockItem ? "minecraft:block/block" : "minecraft:item/generated");
        model.add("textures", textures);
        return model;
    }

    private static JsonObject itemInfo(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        JsonObject renderer = new JsonObject();
        renderer.addProperty("type", PropRenderer.ItemRenderer.TYPE.toString());
        renderer.addProperty("prop", id.toString());
        JsonObject special = new JsonObject();
        special.addProperty("type", "minecraft:special");
        special.addProperty("base", ModelLocationUtils.getModelLocation(item).toString());
        special.add("model", renderer);
        JsonObject info = new JsonObject();
        info.add("model", special);
        return info;
    }

    @Override
    public String getName() {
        return "Chiikawa Prop Item Models";
    }
}
