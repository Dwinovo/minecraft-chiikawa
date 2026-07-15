package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class TagData {
    private TagData() {
    }

    // Cross-loader "seeds" convention tag (Fabric/NeoForge common tags). Pulling it
    // into our plant-crops tag means farming mods that tag their seeds the standard
    // way are recognised with no per-mod work. Optional so datagen never fails if
    // nothing populates it.
    private static final TagKey<Item> C_SEEDS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "seeds"));

    public interface TagAppenderProvider<T> {
        TagAppender<T> tag(TagKey<T> key);
    }

    // 26.2 datagen adds tag entries by ResourceKey rather than by value on both loaders.
    private static ResourceKey<Block> key(Block block) {
        return block.builtInRegistryHolder().key();
    }

    private static ResourceKey<Item> key(Item item) {
        return item.builtInRegistryHolder().key();
    }

    private static ResourceKey<EntityType<?>> key(EntityType<?> type) {
        return type.builtInRegistryHolder().key();
    }

    public static void addBlockTags(TagAppenderProvider<Block> tags) {
        tags.tag(InitTag.ENTITY_HARVEST_CROPS)
            .add(key(Blocks.WHEAT), key(Blocks.POTATOES), key(Blocks.CARROTS), key(Blocks.BEETROOTS), key(Blocks.PUMPKIN), key(Blocks.MELON));
        tags.tag(InitTag.ENTITY_DELEVER_CONTAINER)
            .add(key(Blocks.CHEST), key(Blocks.TRAPPED_CHEST), key(Blocks.BARREL), key(Blocks.HOPPER));
    }

    public static void addItemTags(TagAppenderProvider<Item> tags) {
        tags.tag(InitTag.ENTITY_FARMER_TOOLS)
            .add(key(Items.WOODEN_HOE), key(Items.STONE_HOE), key(Items.IRON_HOE), key(Items.GOLDEN_HOE), key(Items.DIAMOND_HOE), key(Items.NETHERITE_HOE));
        tags.tag(InitTag.ENTITY_FENCER_TOOLS)
            .add(key(Items.WOODEN_SWORD), key(Items.STONE_SWORD), key(Items.IRON_SWORD), key(Items.GOLDEN_SWORD), key(Items.DIAMOND_SWORD), key(Items.NETHERITE_SWORD))
            .add(key(InitItems.USAGI_WEAPON.get()))
            .add(key(InitItems.HACHIWARE_WEAPON.get()))
            .add(key(InitItems.CHIIKAWA_WEAPON.get()));
        tags.tag(InitTag.ENTITY_ARCHER_TOOLS)
            .add(key(Items.BOW));
        tags.tag(InitTag.ENTITY_MUSICIAN_TOOLS)
            .add(key(InitItems.MUSIC_BOX.get()));
        tags.tag(InitTag.ENTITY_TAME_FOODS)
            .add(key(Items.APPLE))
            .add(key(Items.BAKED_POTATO))
            .add(key(Items.BREAD))
            .add(key(Items.CARROT))
            .add(key(Items.COOKED_BEEF))
            .add(key(Items.COOKED_CHICKEN))
            .add(key(Items.COOKED_COD))
            .add(key(Items.COOKED_MUTTON))
            .add(key(Items.COOKED_PORKCHOP))
            .add(key(Items.COOKED_RABBIT))
            .add(key(Items.COOKED_SALMON))
            .add(key(Items.COOKIE))
            .add(key(Items.GLOW_BERRIES))
            .add(key(Items.GOLDEN_APPLE))
            .add(key(Items.GOLDEN_CARROT))
            .add(key(Items.HONEY_BOTTLE))
            .add(key(Items.MELON_SLICE))
            .add(key(Items.MUSHROOM_STEW))
            .add(key(Items.PUMPKIN_PIE))
            .add(key(Items.POTATO))
            .add(key(Items.BEETROOT))
            .add(key(Items.RABBIT_STEW))
            .add(key(Items.SWEET_BERRIES));
        tags.tag(InitTag.ENTITY_PLANT_CROPS)
            .add(key(Items.MELON_SEEDS))
            .add(key(Items.PUMPKIN_SEEDS))
            .add(key(Items.WHEAT_SEEDS))
            .add(key(Items.BEETROOT_SEEDS))
            .add(key(Items.POTATO))
            .add(key(Items.CARROT))
            .addOptionalTag(C_SEEDS);
        tags.tag(InitTag.ENTITY_DELIVER_ITEMS)
            .add(key(Items.WHEAT))
            .add(key(Items.BEETROOT))
            .add(key(Items.POTATO))
            .add(key(Items.CARROT))
            .add(key(Items.MELON_SLICE))
            .add(key(Items.PUMPKIN));
        tags.tag(InitTag.ENTITY_PICKABLE_ITEMS)
            .add(key(Items.WHEAT), key(Items.WHEAT_SEEDS))
            .add(key(Items.POTATO))
            .add(key(Items.CARROT))
            .add(key(Items.BEETROOT))
            .add(key(Items.MELON_SLICE))
            .add(key(Items.PUMPKIN));
    }

    public static void addEntityTags(TagAppenderProvider<EntityType<?>> tags) {
        tags.tag(InitTag.ENTITY_HOSTILE_ENTITY)
            .add(key(EntityTypes.BLAZE))
            .add(key(EntityTypes.CAVE_SPIDER))
            .add(key(EntityTypes.DROWNED))
            .add(key(EntityTypes.EVOKER))
            .add(key(EntityTypes.GUARDIAN))
            .add(key(EntityTypes.HUSK))
            .add(key(EntityTypes.ILLUSIONER))
            .add(key(EntityTypes.MAGMA_CUBE))
            .add(key(EntityTypes.PHANTOM))
            .add(key(EntityTypes.PIGLIN))
            .add(key(EntityTypes.PIGLIN_BRUTE))
            .add(key(EntityTypes.PILLAGER))
            .add(key(EntityTypes.SILVERFISH))
            .add(key(EntityTypes.SKELETON))
            .add(key(EntityTypes.SLIME))
            .add(key(EntityTypes.SPIDER))
            .add(key(EntityTypes.STRAY))
            .add(key(EntityTypes.VEX))
            .add(key(EntityTypes.VINDICATOR))
            .add(key(EntityTypes.WITCH))
            .add(key(EntityTypes.WITHER_SKELETON))
            .add(key(EntityTypes.ZOGLIN))
            .add(key(EntityTypes.ZOMBIE));
    }
}
