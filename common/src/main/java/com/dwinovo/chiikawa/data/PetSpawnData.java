package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.spawn.PetSpawn;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The generated wild pet spawns (gameplay doc, section 5): every friend as often as any
 * other, one at a time, out in grassland, savanna, desert, swamp and snow.
 */
public final class PetSpawnData {
    public static final Identifier WILD_PETS = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "wild_pets");
    private static final List<String> BIOMES = List.of(
        "plains", "sunflower_plains", "savanna", "savanna_plateau", "desert", "swamp", "snowy_plains");
    private static final int WEIGHT = 20;

    private PetSpawnData() {
    }

    /** @return spawn lists by id */
    public static Map<Identifier, PetSpawn> all() {
        List<ExtraCodecs.TagOrElementLocation> biomes = BIOMES.stream()
            .map(path -> new ExtraCodecs.TagOrElementLocation(Identifier.withDefaultNamespace(path), false))
            .toList();
        List<Weighted<MobSpawnSettings.SpawnerData>> spawners = Stream.<Supplier<? extends EntityType<?>>>of(
                InitEntity.USAGI_PET, InitEntity.HACHIWARE_PET, InitEntity.CHIIKAWA_PET, InitEntity.SHISA_PET,
                InitEntity.MOMONGA_PET, InitEntity.KURIMANJU_PET, InitEntity.RAKKO_PET, InitEntity.FURUHONYA_PET)
            .map(type -> new Weighted<>(new MobSpawnSettings.SpawnerData(type.get(), 1, 1), WEIGHT))
            .toList();
        return Map.of(WILD_PETS, new PetSpawn(biomes, spawners));
    }
}
