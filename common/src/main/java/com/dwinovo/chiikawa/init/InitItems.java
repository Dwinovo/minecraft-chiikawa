package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.item.BagItem;
import com.dwinovo.chiikawa.item.ChiikawaWeapon;
import com.dwinovo.chiikawa.item.HachiwareWeapon;
import com.dwinovo.chiikawa.item.MusicBoxItem;
import com.dwinovo.chiikawa.item.PetBellItem;
import com.dwinovo.chiikawa.item.PetDollItem;
import com.dwinovo.chiikawa.item.UsagiWeapon;
import com.dwinovo.chiikawa.platform.Services;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class InitItems {
    public static final Supplier<SpawnEggItem> USAGI_SPAWN_EGG =
        registerSpawnEgg("usagi_spawn_egg", InitEntity.USAGI_PET, 0xf28907, 0xF2CB07);
    public static final Supplier<SpawnEggItem> HACHIWARE_SPAWN_EGG =
        registerSpawnEgg("hachiware_spawn_egg", InitEntity.HACHIWARE_PET, 0x00FFFF, 0x89cff0);
    public static final Supplier<SpawnEggItem> CHIIKAWA_SPAWN_EGG =
        registerSpawnEgg("chiikawa_spawn_egg", InitEntity.CHIIKAWA_PET, 0xE7CCCC, 0xEDE8DC);
    public static final Supplier<SpawnEggItem> SHISA_SPAWN_EGG =
        registerSpawnEgg("shisa_spawn_egg", InitEntity.SHISA_PET, 0xFFA500, 0xDFFF00);
    public static final Supplier<SpawnEggItem> MOMONGA_SPAWN_EGG =
        registerSpawnEgg("momonga_spawn_egg", InitEntity.MOMONGA_PET, 0x0ABAB5, 0x00008B);
    public static final Supplier<SpawnEggItem> KURIMANJU_SPAWN_EGG =
        registerSpawnEgg("kurimanju_spawn_egg", InitEntity.KURIMANJU_PET, 0xdac24e, 0xda8b4e);
    public static final Supplier<SpawnEggItem> RAKKO_SPAWN_EGG =
        registerSpawnEgg("rakko_spawn_egg", InitEntity.RAKKO_PET, 0xeaffd0, 0xeaeaea);
    public static final Supplier<SpawnEggItem> FURUHONYA_SPAWN_EGG =
        registerSpawnEgg("furuhonya_spawn_egg", InitEntity.FURUHONYA_PET, 0x6b4423, 0xc8a165);
    public static final Supplier<Item> USAGI_WEAPON =
        registerItem("usagi_weapon", UsagiWeapon::new);
    public static final Supplier<Item> HACHIWARE_WEAPON =
        registerItem("hachiware_weapon", HachiwareWeapon::new);
    public static final Supplier<Item> CHIIKAWA_WEAPON =
        registerItem("chiikawa_weapon", ChiikawaWeapon::new);
    public static final Supplier<Item> MUSIC_BOX =
        registerItem("music_box", MusicBoxItem::new);
    public static final Supplier<BlockItem> LABOR_BOARD =
        registerPropBlock("labor_board", InitBlocks.LABOR_BOARD);

    public static final Supplier<Item> SIMPLE_DISH =
        registerItem("simple_dish", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> PET_BELL =
        registerItem("pet_bell", () -> new PetBellItem(new Item.Properties()));

    public static final Supplier<Item> BACKPACK =
        registerBag("backpack", BagItem.Wear.ON_BACK);
    public static final Supplier<Item> BEAR_POUCH =
        registerBag("bear_pouch", BagItem.Wear.SLUNG);
    public static final Supplier<Item> WHALE_POUCH =
        registerBag("whale_pouch", BagItem.Wear.SLUNG);
    public static final Supplier<Item> STAR_POUCH =
        registerBag("star_pouch", BagItem.Wear.SLUNG);
    /**
     * Everything drawn from a Bedrock model of its own, by {@code PropRenderer}: the bags
     * and the labor board. Each loader gives these their built-in item renderer, and their
     * item models are generated from this list.
     */
    public static final List<Supplier<? extends Item>> PROPS =
        List.of(BACKPACK, BEAR_POUCH, WHALE_POUCH, STAR_POUCH, LABOR_BOARD);

    public static final Supplier<BlockItem> SHOP =
        registerItem("shop", () -> new BlockItem(InitBlocks.SHOP.get(), new Item.Properties()));
    public static final Supplier<Item> USAGI_DOLL =
        registerItem("usagi_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.USAGI_PET));
    public static final Supplier<Item> HACHIWARE_DOLL =
        registerItem("hachiware_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.HACHIWARE_PET));
    public static final Supplier<Item> CHIIKAWA_DOLL =
        registerItem("chiikawa_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.CHIIKAWA_PET));
    public static final Supplier<Item> SHISA_DOLL =
        registerItem("shisa_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.SHISA_PET));
    public static final Supplier<Item> MOMONGA_DOLL =
        registerItem("momonga_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.MOMONGA_PET));
    public static final Supplier<Item> KURIMANJU_DOLL =
        registerItem("kurimanju_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.KURIMANJU_PET));
    public static final Supplier<Item> RAKKO_DOLL =
        registerItem("rakko_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.RAKKO_PET));
    public static final Supplier<Item> FURUHONYA_DOLL =
        registerItem("furuhonya_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.FURUHONYA_PET));

    private InitItems() {
    }

    public static void init() {
    }

    private static Supplier<SpawnEggItem> registerSpawnEgg(
        String name,
        Supplier<? extends EntityType<? extends Mob>> type,
        int primaryColor,
        int secondaryColor
    ) {
        ResourceLocation id = new ResourceLocation(Constants.MOD_ID, name);
        // 使用平台特定的 registerSpawnEgg 以处理 Forge 上的延迟实体获取
        return Services.REGISTRY.registerSpawnEgg(
            id,
            type,
            primaryColor,
            secondaryColor,
            new Item.Properties()
        );
    }

    // Props are drawn by a built-in item renderer, and Forge 1.20.1 hands an item its client
    // renderer only through the item itself, so the loader makes these.
    private static Supplier<Item> registerBag(String name, BagItem.Wear wear) {
        ResourceLocation id = new ResourceLocation(Constants.MOD_ID, name);
        return Services.REGISTRY.registerBag(id, wear, new Item.Properties());
    }

    private static Supplier<BlockItem> registerPropBlock(String name, Supplier<? extends Block> block) {
        ResourceLocation id = new ResourceLocation(Constants.MOD_ID, name);
        return Services.REGISTRY.registerPropBlockItem(id, block, new Item.Properties());
    }

    private static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory) {
        ResourceLocation id = new ResourceLocation(Constants.MOD_ID, name);
        return Services.REGISTRY.<T>register(
            BuiltInRegistries.ITEM,
            id,
            factory
        );
    }
}
