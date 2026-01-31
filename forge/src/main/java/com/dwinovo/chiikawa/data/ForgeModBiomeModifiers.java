package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.BiomeSpawnData;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

public class ForgeModBiomeModifiers {

    public static void bootstrap(BootstapContext<BiomeModifier> context) {
        var biomes = context.lookup(Registries.BIOME);
        var entities = context.lookup(Registries.ENTITY_TYPE);
        
        // Create a HolderSet for all configured biomes
        HolderSet<Biome> biomeSet = HolderSet.direct(
            BiomeSpawnData.BIOME_KEYS.stream()
                .map(key -> biomes.getOrThrow(key))
                .toList()
        );

        for (BiomeSpawnData.SpawnEntry spawn : BiomeSpawnData.SPAWNS) {
            ResourceKey<BiomeModifier> key = createKey("spawn_" + spawn.entityKey().location().getPath());
            // We need to get the EntityType. Since we are in mod context, we can try to look it up 
            // but for datagen usually we assume it exists or use the key if the API supports it.
            // ForgeBiomeModifiers.AddSpawnsBiomeModifier expects SpawnerData which needs EntityType.
            // We can use ForgeRegistries to get it since our mod is loaded during datagen.
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(spawn.entityKey().location());
            if (entityType == null) {
                // Determine if it is a vanilla entity or mod entity not yet registered? 
                // For mod entities, they should be registered if InitEntity class is loaded and deferred registers are executed.
                // However, datagen usually runs after registry events.
                throw new IllegalStateException("Entity type not found: " + spawn.entityKey().location());
            }

            context.register(key, new ForgeBiomeModifiers.AddSpawnsBiomeModifier(
                biomeSet,
                List.of(new MobSpawnSettings.SpawnerData(entityType, spawn.weight(), spawn.minCount(), spawn.maxCount()))
            ));
        }
    }

    private static ResourceKey<BiomeModifier> createKey(String name) {
        return ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, new ResourceLocation(Constants.MOD_ID, name));
    }
}
