package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.BoardLevels;
import com.dwinovo.chiikawa.task.PetTaskType;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The generated slip types (gameplay doc, section 3). Hunting slips wait for a board
 * upgrade; everything else goes up from the day the board is placed.
 */
public final class PetTaskTypeData {
    public static final ResourceLocation WEEDING = id("weeding");
    public static final ResourceLocation STREET_PERFORMANCE = id("street_performance");
    public static final ResourceLocation MELEE_HUNTING = id("melee_hunting");
    public static final ResourceLocation RANGED_HUNTING = id("ranged_hunting");
    /** The level a board reaches before it dares put hunting up. */
    private static final int HUNTING_LEVEL = 2;

    private PetTaskTypeData() {
    }

    /** @return slip types by id */
    public static Map<ResourceLocation, PetTaskType> all() {
        ResourceLocation farmer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FARMER.get());
        ResourceLocation musician = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.MUSICIAN.get());
        ResourceLocation fencer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FENCER.get());
        ResourceLocation archer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.ARCHER.get());
        return Map.of(
            // Farmers are the most common job, so their slips come up the most. Picking
            // mushrooms is not among them: mushrooms are only there to pick at night, so a
            // slip for them left a farmer standing about all day waiting for the dark.
            WEEDING, new PetTaskType(farmer, PetWorkCounters.WEED, vanilla("short_grass"),
                UniformInt.of(8, 16), reward(WEEDING), 5, BoardLevels.FIRST_LEVEL),
            // Seconds of music.
            STREET_PERFORMANCE, new PetTaskType(musician, PetWorkCounters.PLAY_MUSIC_SECOND, vanilla("note_block"),
                UniformInt.of(120, 240), reward(STREET_PERFORMANCE), 1, BoardLevels.FIRST_LEVEL),
            // Hunting pays the most and asks the most: a pet that falls loses the slip.
            // It waits for an upgrade, so paying a board up buys new work and not only
            // more of the same.
            MELEE_HUNTING, new PetTaskType(fencer, PetWorkCounters.SLAY, vanilla("iron_sword"),
                UniformInt.of(3, 6), reward(MELEE_HUNTING), 2, HUNTING_LEVEL),
            RANGED_HUNTING, new PetTaskType(archer, PetWorkCounters.SLAY, vanilla("bow"),
                UniformInt.of(3, 6), reward(RANGED_HUNTING), 2, HUNTING_LEVEL)
        );
    }

    /** @return the reward loot table of a slip type, {@code <namespace>:pet_task/<path>} */
    public static ResourceKey<LootTable> reward(ResourceLocation type) {
        return ResourceKey.create(Registries.LOOT_TABLE, type.withPrefix("pet_task/"));
    }

    /** What a slip is pictured as: the work it is about, not what it pays. */
    private static ResourceLocation vanilla(String path) {
        return new ResourceLocation(path);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Constants.MOD_ID, path);
    }
}
