package com.dwinovo.chiikawa.platform.services;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public interface IRegistryHelper {
    <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<? extends T> factory);

    <T> Supplier<T> register(ResourceKey<Registry<T>> registryKey, ResourceLocation id, Supplier<? extends T> factory);

    <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, ResourceLocation defaultId, boolean sync);

    void registerToEventBus(Object eventBus);

    java.util.function.Supplier<net.minecraft.world.item.SpawnEggItem> registerSpawnEgg(
        ResourceLocation id,
        Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
        int primaryColor,
        int secondaryColor,
        net.minecraft.world.item.Item.Properties properties
    );

    <T> Iterable<T> getRegistry(ResourceKey<Registry<T>> key);
}
