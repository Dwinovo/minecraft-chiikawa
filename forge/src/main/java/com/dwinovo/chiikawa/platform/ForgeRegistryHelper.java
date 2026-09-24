package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
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
    public void registerPoi(ResourceLocation id, Supplier<? extends Block> block) {
        // Forge ties the listed block states to the type when the entry is registered.
        register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, id,
            () -> new PoiType(ImmutableSet.copyOf(block.get().getStateDefinition().getPossibleStates()), 0, 1));
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(ResourceLocation id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block> block) {
        return register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
            () -> BlockEntityType.Builder.of(factory::apply, block.get()).build(null));
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
    public java.util.function.Supplier<net.minecraft.world.item.Item> registerBag(
        ResourceLocation id,
        com.dwinovo.chiikawa.item.BagItem.Wear wear,
        net.minecraft.world.item.Item.Properties properties
    ) {
        return register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id,
            () -> new com.dwinovo.chiikawa.item.BagItem(properties, wear) {
                @Override
                public void initializeClient(
                        java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
                    // Only called on the client, so the client-only renderer stays off a server.
                    consumer.accept(com.dwinovo.chiikawa.ChiikawaForgeClient.PROP_ITEM_EXTENSIONS);
                }
            });
    }

    @Override
    public java.util.function.Supplier<net.minecraft.world.item.Item> registerHandbook(
        ResourceLocation id,
        net.minecraft.world.item.Item.Properties properties
    ) {
        return register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id,
            () -> new com.dwinovo.chiikawa.item.HandbookItem(properties) {
                @Override
                public void initializeClient(
                        java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
                    consumer.accept(com.dwinovo.chiikawa.ChiikawaForgeClient.PROP_ITEM_EXTENSIONS);
                }
            });
    }

    @Override
    public java.util.function.Supplier<net.minecraft.world.item.BlockItem> registerPropBlockItem(
        ResourceLocation id,
        Supplier<? extends Block> block,
        net.minecraft.world.item.Item.Properties properties
    ) {
        return register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id,
            () -> new net.minecraft.world.item.BlockItem(block.get(), properties) {
                @Override
                public void initializeClient(
                        java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
                    consumer.accept(com.dwinovo.chiikawa.ChiikawaForgeClient.PROP_ITEM_EXTENSIONS);
                }
            });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> getRegistry(ResourceKey<Registry<T>> key) {
        return (Iterable<T>) net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(key.location());
    }

    @Override
    public <T> ResourceLocation getKey(ResourceKey<Registry<T>> key, T value) {
        return net.minecraftforge.registries.RegistryManager.ACTIVE.<T>getRegistry(key.location()).getKey(value);
    }

    @Override
    public <T> boolean containsKey(ResourceKey<Registry<T>> key, ResourceLocation id) {
        return net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(key.location()).containsKey(id);
    }
}
