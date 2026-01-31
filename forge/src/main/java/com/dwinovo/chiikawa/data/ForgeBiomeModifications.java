package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles biome modifications for creature spawning in Forge.
 * Uses events to add spawn rules to biomes.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeBiomeModifications {
    private ForgeBiomeModifications() {
    }

    // Note: In Forge 1.20.1, biome modifications are typically done via JSON files in
    // data/<modid>/forge/biome_modifier/ directory, or via BiomeLoadingEvent.
    // For complex spawning rules, create biome modifier JSON files.
    // This class is kept for potential future event-based modifications.
}
