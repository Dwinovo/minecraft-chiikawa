package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class InitTabs {
    public static final Supplier<CreativeModeTab> MAIN =
        Services.REGISTRY.<CreativeModeTab>register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "main"),
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.chiikawa"))
                .icon(() -> new ItemStack(InitItems.USAGI_SPAWN_EGG.get()))
                .displayItems((parameters, output) -> {
                    output.accept(InitItems.USAGI_SPAWN_EGG.get());
                    output.accept(InitItems.HACHIWARE_SPAWN_EGG.get());
                    output.accept(InitItems.CHIIKAWA_SPAWN_EGG.get());
                    output.accept(InitItems.SHISA_SPAWN_EGG.get());
                    output.accept(InitItems.MOMONGA_SPAWN_EGG.get());
                    output.accept(InitItems.KURIMANJU_SPAWN_EGG.get());
                    output.accept(InitItems.RAKKO_SPAWN_EGG.get());
                    output.accept(InitItems.USAGI_DOLL.get());
                    output.accept(InitItems.HACHIWARE_DOLL.get());
                    output.accept(InitItems.CHIIKAWA_DOLL.get());
                    output.accept(InitItems.SHISA_DOLL.get());
                    output.accept(InitItems.MOMONGA_DOLL.get());
                    output.accept(InitItems.KURIMANJU_DOLL.get());
                    output.accept(InitItems.RAKKO_DOLL.get());
                    output.accept(InitItems.USAGI_WEAPON.get());
                    output.accept(InitItems.HACHIWARE_WEAPON.get());
                    output.accept(InitItems.CHIIKAWA_WEAPON.get());
                })
                .build()
        );

    private InitTabs() {
    }

    public static void init() {
    }
}
