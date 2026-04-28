package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class NeoForgeRegistryHelper implements IRegistryHelper {
    private final Map<Registry<?>, DeferredRegister<?>> deferredRegisters = new HashMap<>();
    private final List<Registry<?>> customRegistries = new ArrayList<>();

    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, Identifier id, Supplier<? extends T> factory) {
        DeferredRegister<T> deferredRegister = getDeferredRegister(registry);
        if (!Constants.MOD_ID.equals(id.getNamespace())) {
            throw new IllegalArgumentException("Unexpected namespace for registry entry: " + id);
        }
        return deferredRegister.register(id.getPath(), factory);
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, Identifier defaultId, boolean sync) {
        RegistryBuilder<T> builder = new RegistryBuilder<>(key);
        if (sync) {
            builder.sync(true);
        }
        if (defaultId != null) {
            builder.defaultKey(defaultId);
        }
        Registry<T> registry = builder.create();
        customRegistries.add(registry);
        return registry;
    }

    @Override
    public void registerToEventBus(Object eventBus) {
        IEventBus bus = (IEventBus) eventBus;
        if (!customRegistries.isEmpty()) {
            bus.addListener((NewRegistryEvent event) -> {
                for (Registry<?> registry : customRegistries) {
                    event.register(registry);
                }
            });
        }
        for (DeferredRegister<?> deferredRegister : deferredRegisters.values()) {
            deferredRegister.register(bus);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> DeferredRegister<T> getDeferredRegister(Registry<? super T> registry) {
        return (DeferredRegister<T>) deferredRegisters.computeIfAbsent(
            registry,
            value -> DeferredRegister.create((Registry<T>) value, Constants.MOD_ID)
        );
    }
}
