package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSwitchLog;
import com.dwinovo.chiikawa.entity.brain.intent.RunningIntent;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.social.InteractionPlan;
import com.dwinovo.chiikawa.social.InteractionReservation;
import com.dwinovo.chiikawa.social.SocialCooldowns;
import com.dwinovo.chiikawa.task.FinishedSlip;
import com.dwinovo.chiikawa.voice.PetSpeech;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "weed_pos"),
            () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC))
        );

    public static final Supplier<MemoryModuleType<BlockPos>> MUSHROOM_POS =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "mushroom_pos"),
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

    /** The labor board nearest the pet. */
    public static final Supplier<MemoryModuleType<BlockPos>> NEAREST_BOARD =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "nearest_board"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** The shop nearest the pet. */
    public static final Supplier<MemoryModuleType<BlockPos>> NEAREST_SHOP =
        Services.REGISTRY.<MemoryModuleType<BlockPos>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "nearest_shop"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Present, with an expiry, after the pet has bought something, so it does not buy the shop out. */
    public static final Supplier<MemoryModuleType<Unit>> SHOP_COOLDOWN =
        Services.REGISTRY.<MemoryModuleType<Unit>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "shop_cooldown"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Present, with an expiry, after the pet failed to take a slip, so it does not retry at once. */
    public static final Supplier<MemoryModuleType<Unit>> TAKE_TASK_COOLDOWN =
        Services.REGISTRY.<MemoryModuleType<Unit>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "take_task_cooldown"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** The last slip the pet was paid for, and when. */
    public static final Supplier<MemoryModuleType<FinishedSlip>> LAST_FINISHED_SLIP =
        Services.REGISTRY.<MemoryModuleType<FinishedSlip>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "last_finished_slip"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** A scene the pet means to play with another pet, see {@link InteractionPlan}. */
    public static final Supplier<MemoryModuleType<InteractionPlan>> INTERACTION_PLAN =
        Services.REGISTRY.<MemoryModuleType<InteractionPlan>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "interaction_plan"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Present, with an expiry, while another pet is on its way to play a scene with this one. */
    public static final Supplier<MemoryModuleType<InteractionReservation>> INTERACTION_RESERVATION =
        Services.REGISTRY.<MemoryModuleType<InteractionReservation>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "interaction_reservation"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Which scenes the pet has lately played with whom. */
    public static final Supplier<MemoryModuleType<SocialCooldowns>> SOCIAL_COOLDOWNS =
        Services.REGISTRY.<MemoryModuleType<SocialCooldowns>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "social_cooldowns"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** The intent the selector last chose. */
    public static final Supplier<MemoryModuleType<RunningIntent>> CURRENT_INTENT =
        Services.REGISTRY.<MemoryModuleType<RunningIntent>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "current_intent"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Present when the selector should evaluate on the next tick instead of waiting for its interval. */
    public static final Supplier<MemoryModuleType<Unit>> INTENT_REEVALUATE =
        Services.REGISTRY.<MemoryModuleType<Unit>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "intent_reevaluate"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** Recent intent switches; present only while logging is turned on by the debug command. */
    public static final Supplier<MemoryModuleType<IntentSwitchLog>> INTENT_SWITCH_LOG =
        Services.REGISTRY.<MemoryModuleType<IntentSwitchLog>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "intent_switch_log"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    /** What the pet last said out loud and when; see {@link PetSpeech}. */
    public static final Supplier<MemoryModuleType<PetSpeech.Said>> LAST_SAID =
        Services.REGISTRY.<MemoryModuleType<PetSpeech.Said>>register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "last_said"),
            () -> new MemoryModuleType<>(Optional.empty())
        );

    private InitMemory() {
    }

    public static void init() {
    }
}
