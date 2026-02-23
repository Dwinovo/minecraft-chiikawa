package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.item.ChiikawaWeapon;
import com.dwinovo.chiikawa.item.HachiwareWeapon;
import com.dwinovo.chiikawa.item.PetDollItem;
import com.dwinovo.chiikawa.item.UsagiWeapon;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

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
    public static final Supplier<Item> USAGI_WEAPON =
        registerItem("usagi_weapon", UsagiWeapon::new);
    public static final Supplier<Item> HACHIWARE_WEAPON =
        registerItem("hachiware_weapon", HachiwareWeapon::new);
    public static final Supplier<Item> CHIIKAWA_WEAPON =
        registerItem("chiikawa_weapon", ChiikawaWeapon::new);
    public static final Supplier<Item> USAGI_DOLL =
        registerItem("usagi_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.USAGI_PET));
    public static final Supplier<Item> HACHIWARE_DOLL =
        registerItem("hachiware_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.HACHIWARE_PET));
    public static final Supplier<Item> CHIIKAWA_DOLL =
        registerItem("chiikawa_doll", () -> new PetDollItem(new Item.Properties(), InitEntity.CHIIKAWA_PET));

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

    private static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory) {
        ResourceLocation id = new ResourceLocation(Constants.MOD_ID, name);
        return Services.REGISTRY.<T>register(
            BuiltInRegistries.ITEM,
            id,
            factory
        );
    }
}
