package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.Services;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public final class InitMemory {
    public static final Supplier<MemoryModuleType<BlockPos>> HARVEST_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            new ResourceLocation(Constants.MOD_ID, "harvest_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<BlockPos>> PLANT_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            new ResourceLocation(Constants.MOD_ID, "plant_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<BlockPos>> CONTAINER_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            new ResourceLocation(Constants.MOD_ID, "container_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<net.minecraft.world.entity.item.ItemEntity>> PICKABLE_ITEM =
        Services.REGISTRY.<MemoryModuleType<net.minecraft.world.entity.item.ItemEntity>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            new ResourceLocation(Constants.MOD_ID, "pickable_item"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    private InitMemory() {
    }

    public static void init() {
    }
}
