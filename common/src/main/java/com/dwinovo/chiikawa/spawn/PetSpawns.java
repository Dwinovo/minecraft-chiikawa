package com.dwinovo.chiikawa.spawn;

import com.dwinovo.chiikawa.Constants;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The wild pet spawns currently loaded from data packs. Server-side only; replaced as a
 * whole on every data pack (re)load, and read by each loader when the server starts and it
 * fills in its biomes.
 */
public final class PetSpawns {
    private static volatile SortedMap<ResourceLocation, PetSpawn> byId = Collections.emptySortedMap();

    private PetSpawns() {
    }

    /** @return every loaded spawn list by id */
    public static SortedMap<ResourceLocation, PetSpawn> all() {
        return byId;
    }

    /**
     * Who spawns in a biome, from every list that names it.
     *
     * @param biome the biome's key
     * @param hasTag whether the biome is in a tag, as the loader asking can tell
     */
    public static List<Weighted<MobSpawnSettings.SpawnerData>> in(ResourceKey<Biome> biome, Predicate<TagKey<Biome>> hasTag) {
        List<Weighted<MobSpawnSettings.SpawnerData>> found = byId.values().stream()
            .filter(spawn -> spawn.covers(biome, hasTag))
            .flatMap(spawn -> spawn.spawners().stream())
            .toList();
        if (!found.isEmpty()) {
            Constants.LOG.debug("[chiikawa-spawn] {} spawns {}", biome.location(),
                found.stream().map(spawner -> BuiltInRegistries.ENTITY_TYPE.getKey(spawner.value().type())).toList());
        }
        return found;
    }

    static void replaceAll(Map<ResourceLocation, PetSpawn> spawns) {
        byId = Collections.unmodifiableSortedMap(new TreeMap<>(spawns));
    }
}
