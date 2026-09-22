package com.dwinovo.chiikawa.spawn;

import com.dwinovo.chiikawa.Constants;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Adds the wild pet spawns in the loaded {@code pet_spawn} data to the biomes they name.
 * The mod's one biome modifier, {@code chiikawa:pet_spawns}, is only this: the spawns
 * themselves are in {@code pet_spawn}, the same files Fabric reads, so a pack changes them
 * there on either loader. NeoForge runs biome modifiers when the server starts, after the
 * data packs have loaded.
 */
public final class PetSpawnsBiomeModifier implements BiomeModifier {
    public static final PetSpawnsBiomeModifier INSTANCE = new PetSpawnsBiomeModifier();
    public static final MapCodec<PetSpawnsBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Constants.MOD_ID);

    static {
        SERIALIZERS.register("pet_spawns", () -> CODEC);
    }

    private PetSpawnsBiomeModifier() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }
        biome.unwrapKey().ifPresent(key -> PetSpawns.in(key, biome::is)
            .forEach(spawner -> builder.getMobSpawnSettings().addSpawn(spawner.type.getCategory(), spawner)));
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
