package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.spawn.PetSpawnsBiomeModifier;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The mod's one biome modifier, which adds the spawns in the {@code pet_spawn} data: a pack
 * changes spawns there, on either loader, rather than here.
 */
public final class ForgeModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> PET_SPAWNS = ResourceKey.create(
            ForgeRegistries.Keys.BIOME_MODIFIERS,
            new ResourceLocation(Constants.MOD_ID, "pet_spawns"));

    private ForgeModBiomeModifiers() {
    }

    public static void bootstrap(BootstapContext<BiomeModifier> context) {
        context.register(PET_SPAWNS, PetSpawnsBiomeModifier.INSTANCE);
    }
}
