package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public class FabricRegistryHelper implements IRegistryHelper {
    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, Identifier id, Supplier<? extends T> factory) {
        T value = factory.get();
        Registry.register(registry, id, value);
        return () -> value;
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, Identifier defaultId, boolean sync) {
        var builder = defaultId == null
            ? FabricRegistryBuilder.create(key)
            : FabricRegistryBuilder.createDefaulted(key, defaultId);
        if (sync) {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        return builder.buildAndRegister();
    }

    @Override
    public void registerToEventBus(Object eventBus) {
        // Fabric doesn't use a mod event bus for registry entries.
    }
}
