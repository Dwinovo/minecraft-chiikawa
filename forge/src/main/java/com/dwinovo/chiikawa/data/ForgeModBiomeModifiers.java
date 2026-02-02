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

public class ForgeModBiomeModifiers {

    public static void bootstrap(BootstapContext<BiomeModifier> context) {
        var biomes = context.lookup(Registries.BIOME);
        var entities = context.lookup(Registries.ENTITY_TYPE);

        HolderSet<Biome> biomeSet = HolderSet.direct(
            BiomeSpawnData.BIOME_KEYS.stream()
                .map(biomes::getOrThrow)
                .toList()
        );

        for (BiomeSpawnData.SpawnEntry spawn : BiomeSpawnData.SPAWNS) {
            ResourceKey<BiomeModifier> key = ResourceKey.create(
                ForgeRegistries.Keys.BIOME_MODIFIERS,
                new ResourceLocation(Constants.MOD_ID, "spawn_" + spawn.entityKey().location().getPath())
            );
            EntityType<?> entityType = entities.getOrThrow(spawn.entityKey()).value();
            context.register(
                key,
                ForgeBiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                    biomeSet,
                    new MobSpawnSettings.SpawnerData(entityType, spawn.weight(), spawn.minCount(), spawn.maxCount())
                )
            );
        }
    }

    private ForgeModBiomeModifiers() {
    }
}
