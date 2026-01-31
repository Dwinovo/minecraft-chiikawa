package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

public class ForgeRegistryHelper implements IRegistryHelper {
    // Map from registry key to DeferredRegister
    private final Map<ResourceKey<?>, DeferredRegister<?>> deferredRegisters = new HashMap<>();
    // Map for pending custom registries
    private final Map<ResourceKey<?>, RegistryBuilder<?>> pendingRegistries = new HashMap<>();
    // Map for custom registry keys
    private final Map<ResourceKey<?>, ResourceLocation> customRegistryDefaults = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<? extends T> factory) {
        if (registry == null) {
             throw new IllegalStateException("Registry is null. Use register(ResourceKey, ...) instead.");
        }
        return register((ResourceKey<Registry<T>>) ((Registry<T>)registry).key(), id, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(ResourceKey<Registry<T>> registryKey, ResourceLocation id, Supplier<? extends T> factory) {
        if (!Constants.MOD_ID.equals(id.getNamespace())) {
            throw new IllegalArgumentException("Unexpected namespace for registry entry: " + id);
        }
        
        DeferredRegister<T> deferredRegister = (DeferredRegister<T>) deferredRegisters.computeIfAbsent(
            registryKey,
            key -> DeferredRegister.create((ResourceKey<? extends Registry<T>>) key, Constants.MOD_ID)
        );
        return deferredRegister.register(id.getPath(), factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, ResourceLocation defaultId, boolean sync) {
        // For Forge 1.20.1, we can't directly return a Registry since it must be
        // created during NewRegistryEvent. Instead, store the configuration and return null.
        // The actual registry will be created by ForgePlatformRegistryAccess.
        RegistryBuilder<T> builder = new RegistryBuilder<>();
        builder.setName(key.location());
        if (defaultId != null) {
            builder.setDefaultKey(defaultId);
        }
        pendingRegistries.put(key, builder);
        customRegistryDefaults.put(key, defaultId);
        
        // Return null - Forge requires custom registry entries to be registered
        // through ForgePlatformRegistryAccess instead of InitRegistry
        return null;
    }

    @Override
    public void registerToEventBus(Object eventBus) {
        IEventBus bus = (IEventBus) eventBus;
        
        // Register for NewRegistryEvent
        bus.addListener(this::onNewRegistry);
        
        // Register all standard DeferredRegisters (for vanilla registries)
        for (DeferredRegister<?> deferredRegister : deferredRegisters.values()) {
            deferredRegister.register(bus);
        }
    }

    private void onNewRegistry(NewRegistryEvent event) {
        for (Map.Entry<ResourceKey<?>, RegistryBuilder<?>> entry : pendingRegistries.entrySet()) {
            ResourceKey<?> key = entry.getKey();
            RegistryBuilder<?> builder = entry.getValue();
            event.create(builder);
        }
    }
    @Override
    public java.util.function.Supplier<net.minecraft.world.item.SpawnEggItem> registerSpawnEgg(
        ResourceLocation id,
        Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
        int primaryColor,
        int secondaryColor,
        net.minecraft.world.item.Item.Properties properties
    ) {
        return (Supplier<net.minecraft.world.item.SpawnEggItem>) (Supplier<?>) register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id, 
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(type, primaryColor, secondaryColor, properties));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> getRegistry(ResourceKey<Registry<T>> key) {
        return (Iterable<T>) net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(key.location());
    }
}
