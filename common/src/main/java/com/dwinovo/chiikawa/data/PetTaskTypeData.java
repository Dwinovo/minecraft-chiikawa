package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.PetTaskType;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The generated slip types (gameplay doc, section 3). Hunting slips come with board
 * upgrades.
 */
public final class PetTaskTypeData {
    public static final ResourceLocation WEEDING = id("weeding");
    public static final ResourceLocation MUSHROOM_PICKING = id("mushroom_picking");
    public static final ResourceLocation STREET_PERFORMANCE = id("street_performance");

    private PetTaskTypeData() {
    }

    /** @return slip types by id */
    public static Map<ResourceLocation, PetTaskType> all() {
        ResourceLocation farmer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FARMER.get());
        ResourceLocation musician = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.MUSICIAN.get());
        return Map.of(
            // Farmers are the most common job, so their slips come up the most.
            WEEDING, new PetTaskType(farmer, PetWorkCounters.WEED, vanilla("short_grass"),
                UniformInt.of(8, 16), reward(WEEDING), 3),
            MUSHROOM_PICKING, new PetTaskType(farmer, PetWorkCounters.PICK_MUSHROOM, vanilla("red_mushroom"),
                UniformInt.of(4, 8), reward(MUSHROOM_PICKING), 2),
            // Seconds of music.
            STREET_PERFORMANCE, new PetTaskType(musician, PetWorkCounters.PLAY_MUSIC_SECOND, vanilla("note_block"),
                UniformInt.of(120, 240), reward(STREET_PERFORMANCE), 1)
        );
    }

    /** @return the reward loot table of a slip type, {@code <namespace>:pet_task/<path>} */
    public static ResourceKey<LootTable> reward(ResourceLocation type) {
        return ResourceKey.create(Registries.LOOT_TABLE, type.withPrefix("pet_task/"));
    }

    /** What a slip is pictured as: the work it is about, not what it pays. */
    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
