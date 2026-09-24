package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FabricRegistryHelper implements IRegistryHelper {
    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, Identifier id, Supplier<? extends T> factory) {
        T value = factory.get();
        Registry.register(registry, id, value);
        return () -> value;
    }

    @Override
    public void registerPoi(Identifier id, Supplier<? extends Block> block) {
        // Fabric ties the block's states to the type; a plain registry entry would not.
        PointOfInterestHelper.register(id, 0, 1, block.get());
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(Identifier id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block> block) {
        BlockEntityType<T> type = FabricBlockEntityTypeBuilder.create(factory::apply, block.get()).build();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
        return () -> type;
    }

    @Override
    public <T> EntityDataSerializer<T> registerEntityDataSerializer(Identifier id, EntityDataSerializer<T> serializer) {
        FabricTrackedDataRegistry.register(id, serializer);
        return serializer;
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, Identifier defaultId, boolean sync) {
        var builder = defaultId == null
            ? FabricRegistryBuilder.createSimple(key)
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
