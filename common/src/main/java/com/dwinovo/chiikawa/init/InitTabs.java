package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.Services;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class InitTabs {
    public static final Identifier MAIN_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main");
    public static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, MAIN_ID);

    public static final Supplier<CreativeModeTab> MAIN =
        Services.REGISTRY.<CreativeModeTab>register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            MAIN_ID,
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.chiikawa"))
                .icon(() -> new ItemStack(InitItems.USAGI_SPAWN_EGG.get()))
                .build()
        );

    private static final List<Supplier<? extends ItemLike>> MAIN_ITEMS = List.of(
        InitItems.USAGI_SPAWN_EGG,
        InitItems.HACHIWARE_SPAWN_EGG,
        InitItems.CHIIKAWA_SPAWN_EGG,
        InitItems.SHISA_SPAWN_EGG,
        InitItems.MOMONGA_SPAWN_EGG,
        InitItems.KURIMANJU_SPAWN_EGG,
        InitItems.RAKKO_SPAWN_EGG,
        InitItems.USAGI_DOLL,
        InitItems.HACHIWARE_DOLL,
        InitItems.CHIIKAWA_DOLL,
        InitItems.SHISA_DOLL,
        InitItems.MOMONGA_DOLL,
        InitItems.KURIMANJU_DOLL,
        InitItems.RAKKO_DOLL,
        InitItems.USAGI_WEAPON,
        InitItems.HACHIWARE_WEAPON,
        InitItems.CHIIKAWA_WEAPON
    );

    private InitTabs() {
    }

    public static void init() {
    }

    public static void addMainItems(Consumer<ItemLike> output) {
        MAIN_ITEMS.forEach(item -> output.accept(item.get()));
    }
}
