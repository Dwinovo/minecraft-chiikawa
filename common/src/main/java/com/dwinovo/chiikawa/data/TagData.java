package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceLocation;
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
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds"));

    public interface TagAppenderProvider<T> {
        TagAppender<T, T> tag(TagKey<T> key);
    }

    public static void addBlockTags(TagAppenderProvider<Block> tags) {
        tags.tag(InitTag.ENTITY_HARVEST_CROPS)
            .add(Blocks.WHEAT, Blocks.POTATOES, Blocks.CARROTS, Blocks.BEETROOTS, Blocks.PUMPKIN, Blocks.MELON);
        tags.tag(InitTag.ENTITY_DELEVER_CONTAINER)
            .add(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.HOPPER);
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
        tags.tag(InitTag.ENTITY_PICKABLE_ITEMS)
            .add(Items.WHEAT, Items.WHEAT_SEEDS)
            .add(Items.POTATO)
            .add(Items.CARROT)
            .add(Items.BEETROOT)
            .add(Items.MELON_SLICE)
            .add(Items.PUMPKIN);
    }

    public static void addEntityTags(TagAppenderProvider<EntityType<?>> tags) {
        tags.tag(InitTag.ENTITY_HOSTILE_ENTITY)
            .add(EntityType.BLAZE)
            .add(EntityType.CAVE_SPIDER)
            .add(EntityType.DROWNED)
            .add(EntityType.EVOKER)
            .add(EntityType.GUARDIAN)
            .add(EntityType.HUSK)
            .add(EntityType.ILLUSIONER)
            .add(EntityType.MAGMA_CUBE)
            .add(EntityType.PHANTOM)
            .add(EntityType.PIGLIN)
            .add(EntityType.PIGLIN_BRUTE)
            .add(EntityType.PILLAGER)
            .add(EntityType.SILVERFISH)
            .add(EntityType.SKELETON)
            .add(EntityType.SLIME)
            .add(EntityType.SPIDER)
            .add(EntityType.STRAY)
            .add(EntityType.VEX)
            .add(EntityType.VINDICATOR)
            .add(EntityType.WITCH)
            .add(EntityType.WITHER_SKELETON)
            .add(EntityType.ZOGLIN)
            .add(EntityType.ZOMBIE);
    }
}
