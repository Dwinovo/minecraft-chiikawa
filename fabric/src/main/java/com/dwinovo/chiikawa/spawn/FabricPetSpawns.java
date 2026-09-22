package com.dwinovo.chiikawa.spawn;

import com.dwinovo.chiikawa.Constants;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.resources.ResourceLocation;

/**
 * Adds the wild pet spawns in the loaded {@code pet_spawn} data to the biomes they name
 * when the server starts, after the data packs have loaded. Fabric has no data-pack form
 * for spawns of its own, so it reads the same files NeoForge does.
 */
public final class FabricPetSpawns {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_spawns");

    private FabricPetSpawns() {
    }

    public static void init() {
        BiomeModifications.create(ID).add(ModificationPhase.ADDITIONS, BiomeSelectors.all(),
            (selection, context) -> PetSpawns.in(selection.getBiomeKey(), selection::hasTag)
                .forEach(spawner -> context.getSpawnSettings().addSpawn(spawner.type.getCategory(), spawner)));
    }
}
