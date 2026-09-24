package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
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
        TagAppender<T, T> tag(TagKey<T> key);
    }

    public static void addBlockTags(TagAppenderProvider<Block> tags) {
        tags.tag(InitTag.ENTITY_HARVEST_CROPS)
            .add(Blocks.WHEAT, Blocks.POTATOES, Blocks.CARROTS, Blocks.BEETROOTS, Blocks.PUMPKIN, Blocks.MELON);
        tags.tag(InitTag.ENTITY_DELEVER_CONTAINER)
            .add(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.HOPPER);
        tags.tag(InitTag.ENTITY_WEEDS)
            .add(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN);
        tags.tag(InitTag.ENTITY_MUSHROOMS)
            .add(Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM);
        tags.tag(BlockTags.MINEABLE_WITH_AXE)
            .add(InitBlocks.LABOR_BOARD.get(), InitBlocks.SHOP.get());
    }

    public static void addItemTags(TagAppenderProvider<Item> tags) {
        tags.tag(InitTag.ENTITY_FARMER_TOOLS)
            .add(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        tags.tag(InitTag.ENTITY_FENCER_TOOLS)
            .add(Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD)
            .add(InitItems.USAGI_WEAPON.get())
            .add(InitItems.HACHIWARE_WEAPON.get())
            .add(InitItems.CHIIKAWA_WEAPON.get());
        tags.tag(InitTag.ENTITY_ARCHER_TOOLS)
            .add(Items.BOW);
        tags.tag(InitTag.ENTITY_MUSICIAN_TOOLS)
            .add(InitItems.MUSIC_BOX.get());
        tags.tag(InitTag.ENTITY_TAME_FOODS)
            .add(Items.APPLE)
            .add(Items.BAKED_POTATO)
            .add(Items.BREAD)
            .add(Items.CARROT)
            .add(Items.COOKED_BEEF)
            .add(Items.COOKED_CHICKEN)
            .add(Items.COOKED_COD)
            .add(Items.COOKED_MUTTON)
            .add(Items.COOKED_PORKCHOP)
            .add(Items.COOKED_RABBIT)
            .add(Items.COOKED_SALMON)
            .add(Items.COOKIE)
            .add(Items.GLOW_BERRIES)
            .add(Items.GOLDEN_APPLE)
            .add(Items.GOLDEN_CARROT)
            .add(Items.HONEY_BOTTLE)
            .add(Items.MELON_SLICE)
            .add(Items.MUSHROOM_STEW)
            .add(Items.PUMPKIN_PIE)
            .add(Items.POTATO)
            .add(Items.BEETROOT)
            .add(Items.RABBIT_STEW)
            .add(Items.SWEET_BERRIES);
        tags.tag(InitTag.ENTITY_PLANT_CROPS)
            .add(Items.MELON_SEEDS)
            .add(Items.PUMPKIN_SEEDS)
            .add(Items.WHEAT_SEEDS)
            .add(Items.BEETROOT_SEEDS)
            .add(Items.POTATO)
            .add(Items.CARROT)
            .addOptionalTag(C_SEEDS);
        tags.tag(InitTag.ENTITY_DELIVER_ITEMS)
            .add(Items.WHEAT)
            .add(Items.BEETROOT)
            .add(Items.POTATO)
            .add(Items.CARROT)
            .add(Items.MELON_SLICE)
            .add(Items.PUMPKIN);
        // Emeralds, as the villagers have it; a pack can make money of anything.
        tags.tag(InitTag.CURRENCY)
            .add(Items.EMERALD);
        tags.tag(InitTag.ENTITY_PICKABLE_ITEMS)
            .add(Items.WHEAT, Items.WHEAT_SEEDS)
            .add(Items.POTATO)
            .add(Items.CARROT)
            .add(Items.BEETROOT)
            .add(Items.MELON_SLICE)
            .add(Items.PUMPKIN);
    }

    public static void addEntityTags(TagAppenderProvider<EntityType<?>> tags) {
        // Creepers are in the list now. They are not something a pet with a sword walks
        // up to — a pet's reach is a block and a half, well inside the distance at which a
        // creeper starts swelling — but a pet has to be able to see one to keep away from
        // it, and an archer can answer one from outside the blast.
        tags.tag(InitTag.ENTITY_HOSTILE_ENTITY)
            .add(EntityType.BLAZE, EntityType.BOGGED, EntityType.BREEZE, EntityType.CAVE_SPIDER, EntityType.CREEPER,
            EntityType.DROWNED, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.GUARDIAN, EntityType.HOGLIN,
            EntityType.HUSK,
            EntityType.ILLUSIONER, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.PILLAGER, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME, EntityType.SPIDER, EntityType.STRAY,
            EntityType.VEX, EntityType.VINDICATOR, EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.ZOGLIN,
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER);
    }
}
