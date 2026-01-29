package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class InitTag {
    public static final TagKey<Item> ENTITY_FARMER_TOOLS = tag("entity_farmer_tools");
    public static final TagKey<Item> ENTITY_FENCER_TOOLS = tag("entity_fencer_tools");
    public static final TagKey<Item> ENTITY_ARCHER_TOOLS = tag("entity_archer_tools");
    public static final TagKey<Item> ENTITY_TAME_FOODS = tag("entity_tame_foods");
    public static final TagKey<Item> ENTITY_PLANT_CROPS = tag("entity_plant_crops");
    public static final TagKey<Item> ENTITY_DELIVER_ITEMS = tag("entity_deliver_items");
    public static final TagKey<Item> ENTITY_PICKABLE_ITEMS = tag("entity_pickable_items");

    public static final TagKey<EntityType<?>> ENTITY_HOSTILE_ENTITY = tagEntity("entity_hostile_entity");

    // Blocks pets can harvest, defined by the entity_harvest_crops tag file.
    public static final TagKey<Block> ENTITY_HARVEST_CROPS = tagBlock("entity_harvest_crops");

    public static final TagKey<Block> ENTITY_DELEVER_CONTAINER = tagBlock("entity_delever_container");

    private InitTag() {
    }

    public static void init() {
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(Constants.MOD_ID, name));
    }

    private static TagKey<EntityType<?>> tagEntity(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(Constants.MOD_ID, name));
    }

    private static TagKey<Block> tagBlock(String name) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(Constants.MOD_ID, name));
    }
}
