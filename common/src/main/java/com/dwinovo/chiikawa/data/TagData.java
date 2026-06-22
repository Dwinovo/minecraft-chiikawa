package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
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

    @FunctionalInterface
    public interface TagAppenderProvider<T> {
        void add(TagKey<T> key, T... values);

        /**
         * Includes another tag (referenced by id) into {@code key}. Used to pull in
         * cross-mod convention tags like {@code c:seeds}. Optional include, so datagen
         * won't fail if nothing populates the referenced tag. Default no-op — only
         * providers that need tag-includes (the item provider) override it.
         */
        default void addOptionalTag(TagKey<T> key, ResourceLocation includedTagId) {
        }
    }

    public static void addBlockTags(TagAppenderProvider<Block> tags) {
        tags.add(InitTag.ENTITY_HARVEST_CROPS,
            Blocks.WHEAT, Blocks.POTATOES, Blocks.CARROTS, Blocks.BEETROOTS, Blocks.PUMPKIN, Blocks.MELON);
        tags.add(InitTag.ENTITY_DELEVER_CONTAINER,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.HOPPER);
    }

    public static void addItemTags(TagAppenderProvider<Item> tags) {
        tags.add(InitTag.ENTITY_FARMER_TOOLS,
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        tags.add(InitTag.ENTITY_FENCER_TOOLS,
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD,
            InitItems.USAGI_WEAPON.get(), InitItems.HACHIWARE_WEAPON.get(), InitItems.CHIIKAWA_WEAPON.get());
        tags.add(InitTag.ENTITY_ARCHER_TOOLS, Items.BOW);
        tags.add(InitTag.ENTITY_MUSICIAN_TOOLS, InitItems.MUSIC_BOX.get());
        tags.add(InitTag.ENTITY_TAME_FOODS,
            Items.APPLE, Items.BAKED_POTATO, Items.BREAD, Items.CARROT, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_COD,
            Items.COOKED_MUTTON, Items.COOKED_PORKCHOP, Items.COOKED_RABBIT, Items.COOKED_SALMON, Items.COOKIE, Items.GLOW_BERRIES,
            Items.GOLDEN_APPLE, Items.GOLDEN_CARROT, Items.HONEY_BOTTLE, Items.MELON_SLICE, Items.MUSHROOM_STEW, Items.PUMPKIN_PIE,
            Items.POTATO, Items.BEETROOT, Items.RABBIT_STEW, Items.SWEET_BERRIES);
        tags.add(InitTag.ENTITY_PLANT_CROPS,
            Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.POTATO, Items.CARROT);
        // Pull in the cross-loader seeds convention tag so well-behaved farming mods
        // are recognised without per-mod work (see DefaultCropHandler#isSeed).
        tags.addOptionalTag(InitTag.ENTITY_PLANT_CROPS, ResourceLocation.fromNamespaceAndPath("c", "seeds"));
        tags.add(InitTag.ENTITY_DELIVER_ITEMS,
            Items.WHEAT, Items.BEETROOT, Items.POTATO, Items.CARROT, Items.MELON_SLICE, Items.PUMPKIN);
        tags.add(InitTag.ENTITY_PICKABLE_ITEMS,
            Items.WHEAT, Items.WHEAT_SEEDS, Items.POTATO, Items.CARROT, Items.BEETROOT, Items.MELON_SLICE, Items.PUMPKIN);
    }

    public static void addEntityTags(TagAppenderProvider<EntityType<?>> tags) {
        tags.add(InitTag.ENTITY_HOSTILE_ENTITY,
            EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.DROWNED, EntityType.EVOKER, EntityType.GUARDIAN, EntityType.HUSK,
            EntityType.ILLUSIONER, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.PILLAGER, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME, EntityType.SPIDER, EntityType.STRAY,
            EntityType.VEX, EntityType.VINDICATOR, EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.ZOGLIN,
            EntityType.ZOMBIE);
    }
}
