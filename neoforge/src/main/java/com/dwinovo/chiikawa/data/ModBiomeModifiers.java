package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.BiomeSpawnData;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.HolderSet;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_PET_SPAWNS = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "add_pet_spawns"));

    private ModBiomeModifiers() {
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<EntityType<?>> entities = context.lookup(Registries.ENTITY_TYPE);
        WeightedList.Builder<SpawnerData> spawners = WeightedList.builder();

        for (BiomeSpawnData.SpawnEntry entry : BiomeSpawnData.SPAWNS) {
            spawners.add(
                new SpawnerData(entities.getOrThrow(entry.entityKey()).value(), entry.minCount(), entry.maxCount()),
                entry.weight()
            );
        }

        HolderSet<Biome> targetBiomes = HolderSet.direct(
            BiomeSpawnData.BIOME_KEYS.stream()
                .map(biomes::getOrThrow)
                .toList()
        );
        context.register(ADD_PET_SPAWNS, new AddSpawnsBiomeModifier(targetBiomes, spawners.build()));
    }
}


