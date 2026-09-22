package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
    public void registerPoi(ResourceLocation id, Supplier<? extends Block> block) {
        // Fabric ties the block's states to the type; a plain registry entry would not.
        PointOfInterestHelper.register(id, 0, 1, block.get());
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(ResourceLocation id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block> block) {
        BlockEntityType<T> type = FabricBlockEntityTypeBuilder.create(factory::apply, block.get()).build();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
        return () -> type;
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

    @Override
    public java.util.function.Supplier<net.minecraft.world.item.Item> registerBag(
        ResourceLocation id,
        com.dwinovo.chiikawa.item.BagItem.Wear wear,
        net.minecraft.world.item.Item.Properties properties
    ) {
        return register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id,
            () -> new com.dwinovo.chiikawa.item.BagItem(properties, wear));
    }

    @Override
    public java.util.function.Supplier<net.minecraft.world.item.BlockItem> registerPropBlockItem(
        ResourceLocation id,
        Supplier<? extends Block> block,
        net.minecraft.world.item.Item.Properties properties
    ) {
        return register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id,
            () -> new net.minecraft.world.item.BlockItem(block.get(), properties));
    }

    @Override
    public java.util.function.Supplier<net.minecraft.world.item.Item> registerHandbook(
        ResourceLocation id,
        net.minecraft.world.item.Item.Properties properties
    ) {
        return register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id,
            () -> new com.dwinovo.chiikawa.item.HandbookItem(properties));
    }
}
