package com.dwinovo.chiikawa.platform.services;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public interface IRegistryHelper {
    <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<? extends T> factory);

    <T> Supplier<T> register(ResourceKey<Registry<T>> registryKey, ResourceLocation id, Supplier<? extends T> factory);

    /**
     * Registers a point of interest type covering every state of {@code block}, so the
     * level's {@code PoiManager} tracks where that block is placed. Loaders differ in how
     * a block state is tied to its type.
     *
     * @param block a block registered earlier through {@link #register}
     */
    void registerPoi(ResourceLocation id, Supplier<? extends Block> block);

    /**
     * Registers a block entity type placed by {@code block}. Vanilla keeps its block entity
     * factory type package-private, so each loader builds the type its own way.
     *
     * @param block a block registered earlier through {@link #register}
     */
    <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(ResourceLocation id,
        BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block> block);

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
