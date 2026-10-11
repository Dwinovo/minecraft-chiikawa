package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.spawn.PetSpawn;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The generated wild pet spawns (gameplay doc, section 5): every friend as often as any
 * other, one at a time, out in grassland, savanna, desert, swamp and snow. The places are the
 * conventional biome tags rather than biomes by name, so a modded biome of the same kind counts.
 */
public final class PetSpawnData {
    public static final ResourceLocation WILD_PETS = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "wild_pets");
    /** Every vanilla biome the list used to name is in one of these, on both loaders. */
    private static final List<String> BIOME_TAGS = List.of(
        "is_plains", "is_savanna", "is_desert", "is_swamp", "is_snowy_plains");
    private static final int WEIGHT = 3;

    private PetSpawnData() {
    }

    /** @return where wild pets spawn: the conventional tags of the kinds of biome they live in */
    public static List<ExtraCodecs.TagOrElementLocation> biomes() {
        return BIOME_TAGS.stream()
            .map(path -> new ExtraCodecs.TagOrElementLocation(ResourceLocation.fromNamespaceAndPath("c", path), true))
            .toList();
    }

    /** @return spawn lists by id */
    public static Map<ResourceLocation, PetSpawn> all() {
        List<MobSpawnSettings.SpawnerData> spawners = Stream.<Supplier<? extends EntityType<?>>>of(
                InitEntity.USAGI_PET, InitEntity.HACHIWARE_PET, InitEntity.CHIIKAWA_PET, InitEntity.SHISA_PET,
                InitEntity.MOMONGA_PET, InitEntity.KURIMANJU_PET, InitEntity.RAKKO_PET, InitEntity.FURUHONYA_PET)
            .map(type -> new Weighted<>(new MobSpawnSettings.SpawnerData(type.get(), 1, 1), WEIGHT))
            .toList();
        return Map.of(WILD_PETS, new PetSpawn(biomes(), spawners));
    }
}
