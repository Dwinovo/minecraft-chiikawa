package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.anim.render.PropRenderer;
import com.dwinovo.chiikawa.client.render.PropItemRenderer;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * The item models of the props ({@link InitItems#PROPS}): each hands its item to
 * {@link PropItemRenderer}, and so to {@code PropRenderer} and the prop's own Bedrock model.
 * Shared by both loaders' data generators, so the list and the rule live once; each
 * loader's model provider calls this, since the game has every item model of a mod written
 * by the one provider.
 *
 * <p>A block's item is placed, and lit from the side in a slot, as vanilla places and
 * lights a block; a sword or a tool as vanilla does a handheld item; anything else as
 * vanilla does a flat item. That is the model the special one stands on, which the game
 * takes the placing and the lighting from.
 *
 * <p>A prop that wears out, a weapon, breaks into bits of its own texture, as vanilla's
 * shield and trident do into theirs: the model names the texture as its particle, and
 * {@link PropAtlasProvider} puts the texture onto the block atlas.
 */
public final class PropItemModelProvider {
    private static final ModelTemplate BLOCK_ITEM = new ModelTemplate(
        Optional.of(Identifier.withDefaultNamespace("block/block")), Optional.empty());
    private static final ModelTemplate HANDHELD_ITEM = new ModelTemplate(
        Optional.of(Identifier.withDefaultNamespace("item/handheld")), Optional.empty());
    private static final ModelTemplate FLAT_ITEM = new ModelTemplate(
        Optional.of(Identifier.withDefaultNamespace("item/generated")), Optional.empty());

    private PropItemModelProvider() {
    }

    /**
     * @param items where item model definitions go: the loader's item model generators' own
     * @param models where models go, likewise
     */
    public static void generate(ItemModelOutput items, BiConsumer<Identifier, ModelInstance> models) {
        for (Supplier<? extends Item> prop : InitItems.PROPS) {
            Item item = prop.get();
            ModelTemplate template = item instanceof BlockItem ? BLOCK_ITEM
                : PropRenderer.isHandheld(item) ? HANDHELD_ITEM : FLAT_ITEM;
            TextureMapping textures = new TextureMapping();
            if (wearsOut(item)) {
                textures.putForced(TextureSlot.PARTICLE, new Material(texture(item)));
            }
            Identifier base = template.create(ModelLocationUtils.getModelLocation(item), textures, models);
            items.accept(item, ItemModelUtils.specialModel(base, new PropItemRenderer.Unbaked()));
        }
    }

    static boolean wearsOut(Item item) {
        return item.getDefaultInstance().isDamageableItem();
    }

    /** The prop's own texture, as a sprite: {@code textures/entities/<id>.png}. */
    static Identifier texture(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).withPrefix("entities/");
    }
}
