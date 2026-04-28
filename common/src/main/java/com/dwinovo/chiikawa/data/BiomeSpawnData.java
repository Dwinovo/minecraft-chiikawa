package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

public final class BiomeSpawnData {
    public static final List<ResourceKey<Biome>> BIOME_KEYS = List.of(
        biomeKey("plains"),
        biomeKey("sunflower_plains"),
        biomeKey("savanna"),
        biomeKey("savanna_plateau"),
        biomeKey("desert"),
        biomeKey("swamp"),
        biomeKey("snowy_plains")
    );

    public static final List<SpawnEntry> SPAWNS = List.of(
        new SpawnEntry(entityKey("usagi"), MobCategory.CREATURE, 20, 1, 1),
        new SpawnEntry(entityKey("hachiware"), MobCategory.CREATURE, 20, 1, 1),
        new SpawnEntry(entityKey("chiikawa"), MobCategory.CREATURE, 20, 1, 1),
        new SpawnEntry(entityKey("shisa"), MobCategory.CREATURE, 20, 1, 1),
        new SpawnEntry(entityKey("momonga"), MobCategory.CREATURE, 20, 1, 1),
        new SpawnEntry(entityKey("kurimanju"), MobCategory.CREATURE, 20, 1, 1),
        new SpawnEntry(entityKey("rakko"), MobCategory.CREATURE, 20, 1, 1)
    );

    private BiomeSpawnData() {
    }

    private static ResourceKey<Biome> biomeKey(String path) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("minecraft", path));
    }

    private static ResourceKey<EntityType<?>> entityKey(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
    }

    public record SpawnEntry(ResourceKey<EntityType<?>> entityKey, MobCategory category, int weight, int minCount, int maxCount) {
    }
}
