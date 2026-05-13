package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.PetCommand;
import com.dwinovo.chiikawa.platform.Services;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public final class InitMemory {
    public static final Supplier<MemoryModuleType<BlockPos>> HARVEST_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "harvest_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<BlockPos>> PLANT_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plant_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<BlockPos>> CONTAINER_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "container_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<net.minecraft.world.entity.item.ItemEntity>> PICKABLE_ITEM =
        Services.REGISTRY.<MemoryModuleType<net.minecraft.world.entity.item.ItemEntity>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pickable_item"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    public static final Supplier<MemoryModuleType<PetCommand>> REQUESTED_COMMAND =
        Services.REGISTRY.<MemoryModuleType<PetCommand>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "requested_command"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    public static final Supplier<MemoryModuleType<String>> REQUESTED_MUSIC_TRACK =
        Services.REGISTRY.<MemoryModuleType<String>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "requested_music_track"),
            () -> new MemoryModuleType<>(Optional.of(Codec.STRING))
        );

    public static final Supplier<MemoryModuleType<String>> MUSICIAN_LAST_MUSIC_SIGNATURE =
        Services.REGISTRY.<MemoryModuleType<String>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "musician_last_music_signature"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    private InitMemory() {
    }

    public static void init() {
    }
}
