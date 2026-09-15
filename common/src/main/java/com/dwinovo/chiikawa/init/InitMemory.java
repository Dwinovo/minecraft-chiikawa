package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSwitchLog;
import com.dwinovo.chiikawa.entity.brain.intent.RunningIntent;
import com.dwinovo.chiikawa.platform.Services;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
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

    public static final Supplier<MemoryModuleType<BlockPos>> WEED_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "weed_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<BlockPos>> MUSHROOM_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "mushroom_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<net.minecraft.world.entity.item.ItemEntity>> PICKABLE_ITEM =
        Services.REGISTRY.<MemoryModuleType<net.minecraft.world.entity.item.ItemEntity>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pickable_item"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Signature of the music box selection the musician last started, so each selection plays once. */
    public static final Supplier<MemoryModuleType<String>> MUSICIAN_LAST_MUSIC_SIGNATURE =
        Services.REGISTRY.<MemoryModuleType<String>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "musician_last_music_signature"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** The intent the selector last chose. */
    public static final Supplier<MemoryModuleType<RunningIntent>> CURRENT_INTENT =
        Services.REGISTRY.<MemoryModuleType<RunningIntent>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "current_intent"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Present when the selector should evaluate on the next tick instead of waiting for its interval. */
    public static final Supplier<MemoryModuleType<Unit>> INTENT_REEVALUATE =
        Services.REGISTRY.<MemoryModuleType<Unit>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "intent_reevaluate"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Recent intent switches; present only while logging is turned on by the debug command. */
    public static final Supplier<MemoryModuleType<IntentSwitchLog>> INTENT_SWITCH_LOG =
        Services.REGISTRY.<MemoryModuleType<IntentSwitchLog>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "intent_switch_log"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    private InitMemory() {
    }

    public static void init() {
    }
}
