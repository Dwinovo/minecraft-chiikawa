package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableSet;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class NeoForgeRegistryHelper implements IRegistryHelper {
    private final Map<Registry<?>, DeferredRegister<?>> deferredRegisters = new HashMap<>();
    private final List<Registry<?>> customRegistries = new ArrayList<>();

    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<? extends T> factory) {
        DeferredRegister<T> deferredRegister = getDeferredRegister(registry);
        if (!Constants.MOD_ID.equals(id.getNamespace())) {
            throw new IllegalArgumentException("Unexpected namespace for registry entry: " + id);
        }
        return deferredRegister.register(id.getPath(), factory);
    }

    @Override
    public void registerPoi(ResourceLocation id, Supplier<? extends Block> block) {
        // NeoForge ties the listed block states to the type when the entry is registered.
        register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, id,
            () -> new PoiType(ImmutableSet.copyOf(block.get().getStateDefinition().getPossibleStates()), 0, 1));
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(ResourceLocation id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block> block) {
        return register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
            () -> new BlockEntityType<>(factory::apply, block.get()));
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, ResourceLocation defaultId, boolean sync) {
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
