package com.dwinovo.chiikawa.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PetSpawnTest {
    private static final ResourceKey<Biome> PLAINS = biome("plains");
    private static final ResourceKey<Biome> FOREST = biome("forest");
    private static final TagKey<Biome> IS_FOREST = TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("is_forest"));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void aPackNamesBiomesByIdOrByTagAndWritesSpawnersAsVanillaDoes() {
        PetSpawn spawn = parse("""
            { "biomes": [ "minecraft:plains", "#minecraft:is_forest" ],
              "spawners": [ { "type": "minecraft:pig", "weight": 5, "minCount": 1, "maxCount": 2 } ] }""");

        assertTrue(spawn.covers(PLAINS, tag -> false), "named by id");
        assertTrue(spawn.covers(FOREST, IS_FOREST::equals), "named by tag");
        assertFalse(spawn.covers(biome("desert"), tag -> false));
        assertEquals(EntityType.PIG, spawn.spawners().get(0).type);
        assertEquals(2, spawn.spawners().get(0).maxCount);
    }

    @Test
    void everyListNamingABiomeAddsItsSpawners() {
        PetSpawns.replaceAll(Map.of(
            id("a"), parse("""
                { "biomes": [ "minecraft:plains" ],
                  "spawners": [ { "type": "minecraft:pig", "weight": 5, "minCount": 1, "maxCount": 1 } ] }"""),
            id("b"), parse("""
                { "biomes": [ "minecraft:plains", "minecraft:forest" ],
                  "spawners": [ { "type": "minecraft:cow", "weight": 5, "minCount": 1, "maxCount": 1 } ] }""")));

        assertEquals(2, PetSpawns.in(PLAINS, tag -> false).size());
        assertEquals(1, PetSpawns.in(FOREST, tag -> false).size());
        assertEquals(0, PetSpawns.in(biome("desert"), tag -> false).size());
        PetSpawns.replaceAll(Map.of());
    }

    @Test
    void aBrokenFileIsSkippedAndTheRestStillSpawn() {
        PetSpawnLoader.Loaded loaded = PetSpawnLoader.load(Map.of(
            id("good"), JsonParser.parseString("""
                { "biomes": [ "minecraft:plains" ],
                  "spawners": [ { "type": "minecraft:pig", "weight": 5, "minCount": 1, "maxCount": 1 } ] }"""),
            id("backwards"), JsonParser.parseString("""
                { "biomes": [ "minecraft:plains" ],
                  "spawners": [ { "type": "minecraft:pig", "weight": 5, "minCount": 3, "maxCount": 1 } ] }""")));

        assertEquals(1, loaded.spawns().size());
        assertEquals(1, loaded.errors().size());
    }

    private static PetSpawn parse(String json) {
        return PetSpawn.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chiikawa", path);
    }
}
