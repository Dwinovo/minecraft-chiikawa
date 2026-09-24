package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.client.render.PropItemRenderer;
import com.dwinovo.chiikawa.init.InitItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
 * special model renderer", which hands it to {@code PropRenderer} and the prop's own Bedrock
 * model. Shared by both loaders' data generators, so the list and the rule live once.
 *
 * <p>A block's item is lit from the side in a slot, as vanilla lights a block; anything
 * else from the front, as vanilla lights a flat item. The model under the special renderer
 * has no display transforms of its own: {@code PropRenderer} puts the prop where vanilla
 * would put an item of its kind, as it did from a built-in renderer.
 *
 * <p>A prop that wears out, a weapon, breaks into bits of its own texture, as vanilla's
 * shield and trident do into theirs: the model names the texture as its particle, and the
 * texture goes onto the block atlas, where particles are drawn from, the way vanilla puts
 * its own entity textures there ({@code minecraft:atlases/blocks.json}, which every pack
 * adds to).
 */
public final class PropItemModelProvider implements DataProvider {
    private static final ResourceLocation BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("blocks");
    private final PackOutput.PathProvider models;
    private final PackOutput.PathProvider items;
    private final PackOutput.PathProvider atlases;

    public PropItemModelProvider(PackOutput output) {
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.items = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.atlases = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "atlases");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<Item> props = InitItems.PROPS.stream().<Item>map(Supplier::get).toList();
        Stream<CompletableFuture<?>> itemModels = props.stream()
            .flatMap(item -> Stream.of(
                DataProvider.saveStable(cache, model(item), models.json(ModelLocationUtils.getModelLocation(item))),
                DataProvider.saveStable(cache, definition(item), items.json(BuiltInRegistries.ITEM.getKey(item)))));
        return CompletableFuture.allOf(Stream.concat(itemModels, Stream.of(DataProvider.saveStable(cache, atlas(props),
            atlases.json(BLOCK_ATLAS)))).toArray(CompletableFuture[]::new));
    }

    /**
     * A block's item takes its block's model, for the texture of the bits that fly off it;
     * anything else is a model with nothing in it but how it is lit.
     */
    private static JsonObject model(Item item) {
        JsonObject model = new JsonObject();
        if (item instanceof BlockItem block) {
            model.addProperty("parent", ModelLocationUtils.getModelLocation(block.getBlock()).toString());
        } else {
            model.addProperty("gui_light", "front");
        }
        if (wearsOut(item)) {
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", texture(item).toString());
            model.add("textures", textures);
        }
        return model;
    }

    /** The item definition: the prop's special renderer, naming the item, over its item model. */
    private static JsonObject definition(Item item) {
        JsonObject renderer = new JsonObject();
        renderer.addProperty("type", PropItemRenderer.ID.toString());
        renderer.addProperty("item", BuiltInRegistries.ITEM.getKey(item).toString());
        JsonObject special = new JsonObject();
        special.addProperty("type", "minecraft:special");
        special.addProperty("base", ModelLocationUtils.getModelLocation(item).toString());
        special.add("model", renderer);
        JsonObject definition = new JsonObject();
        definition.add("model", special);
        return definition;
    }

    /** The textures of the props that break into bits of them, one sprite each. */
    private static JsonObject atlas(List<Item> props) {
        JsonArray sources = new JsonArray();
        props.stream().filter(PropItemModelProvider::wearsOut).forEach(item -> {
            JsonObject source = new JsonObject();
            source.addProperty("type", "single");
            source.addProperty("resource", texture(item).toString());
            sources.add(source);
        });
        JsonObject atlas = new JsonObject();
        atlas.add("sources", sources);
        return atlas;
    }

    private static boolean wearsOut(Item item) {
        return item.getDefaultInstance().isDamageableItem();
    }

    /** The prop's own texture, as a sprite: {@code textures/entities/<id>.png}. */
    private static ResourceLocation texture(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).withPrefix("entities/");
    }

    @Override
    public String getName() {
        return "Chiikawa Prop Item Models";
    }
}
