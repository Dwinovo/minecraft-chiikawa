package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class FabricRegistryHelper implements IRegistryHelper {
    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<? extends T> factory) {
        T value = factory.get();
        Registry.register(registry, id, value);
        return () -> value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(ResourceKey<Registry<T>> registryKey, ResourceLocation id, Supplier<? extends T> factory) {
        Registry<T> registry = (Registry<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(registryKey.location());
        if (registry == null) {
            throw new IllegalStateException("Registry not found for key: " + registryKey);
        }
        return register(registry, id, factory);
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, ResourceLocation defaultId, boolean sync) {
        var builder = defaultId == null
            ? FabricRegistryBuilder.createSimple(key)
            : FabricRegistryBuilder.createDefaulted(key, defaultId);
        if (sync) {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        return builder.buildAndRegister();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> getRegistry(ResourceKey<Registry<T>> key) {
        return (Iterable<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(key.location());
    }

    @Override
    public void registerToEventBus(Object eventBus) {
        // Fabric doesn't use a mod event bus for registry entries.
    }
    @Override
    @SuppressWarnings("unchecked")
    public java.util.function.Supplier<net.minecraft.world.item.SpawnEggItem> registerSpawnEgg(
        ResourceLocation id,
        Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
        int primaryColor,
        int secondaryColor,
        net.minecraft.world.item.Item.Properties properties
    ) {
         return (Supplier<net.minecraft.world.item.SpawnEggItem>) (Supplier<?>) register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id, 
            () -> new net.minecraft.world.item.SpawnEggItem(type.get(), primaryColor, secondaryColor, properties));
    }
}
