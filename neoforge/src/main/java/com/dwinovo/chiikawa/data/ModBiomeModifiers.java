package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.spawn.PetSpawnsBiomeModifier;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The mod's one biome modifier, which adds the spawns in the {@code pet_spawn} data: a pack
 * changes spawns there, on either loader, rather than here.
 */
public final class ModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> PET_SPAWNS = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_spawns"));

    private ModBiomeModifiers() {
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        context.register(PET_SPAWNS, PetSpawnsBiomeModifier.INSTANCE);
    }
}
