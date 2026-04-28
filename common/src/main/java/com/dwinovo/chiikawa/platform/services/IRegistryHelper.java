package com.dwinovo.chiikawa.platform.services;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public interface IRegistryHelper {
    <T> Supplier<T> register(Registry<? super T> registry, Identifier id, Supplier<? extends T> factory);

    <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, Identifier defaultId, boolean sync);

    void registerToEventBus(Object eventBus);
}
