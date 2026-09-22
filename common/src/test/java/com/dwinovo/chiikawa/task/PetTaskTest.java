package com.dwinovo.chiikawa.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PetTaskTest {
    private static final ResourceLocation FARMER = id("farmer");
    private static final ResourceLocation GRASS = ResourceLocation.withDefaultNamespace("short_grass");
    private static final PetTask WEEDING = new PetTask(id("weeding"), FARMER, PetWorkCounters.WEED, GRASS, 3,
        ResourceKey.create(Registries.LOOT_TABLE, id("pet_task/weeding")), 0);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void advancingCountsWorkUpToTheTarget() {
        PetTask twoDone = WEEDING.advance(2);
        assertEquals(2, twoDone.progress());
        assertFalse(twoDone.isDone());

        PetTask overDone = twoDone.advance(5);
        assertEquals(3, overDone.progress());
        assertTrue(overDone.isDone());
    }

    @Test
    void aSlipSurvivesSavingWithItsProgress() {
        PetTask saved = WEEDING.advance(1);
        assertEquals(saved, PetTask.CODEC.parse(NbtOps.INSTANCE, PetTask.CODEC.encodeStart(NbtOps.INSTANCE, saved).getOrThrow())
            .getOrThrow());
    }

    @Test
    void aTypeRollsAFreshSlipOfItsKind() {
        PetTaskType type = new PetTaskType(FARMER, PetWorkCounters.WEED, GRASS, UniformInt.of(4, 4),
            WEEDING.reward(), 1, BoardLevels.FIRST_LEVEL);

        assertEquals(new PetTask(id("weeding"), FARMER, PetWorkCounters.WEED, GRASS, 4, WEEDING.reward(), 0),
            type.roll(id("weeding"), RandomSource.create(1L)));
    }

    @Test
    void typeFilesParseAndDefaultTheirWeight() {
        PetTaskType type = PetTaskType.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "capability": "chiikawa:farmer", "counter": "chiikawa:weed", "icon": "minecraft:short_grass",
              "amount": { "type": "minecraft:uniform", "min_inclusive": 8, "max_inclusive": 16 },
              "reward": "chiikawa:pet_task/weeding" }""")).getOrThrow();

        assertEquals(1, type.weight());
        assertEquals(FARMER, type.capability());
    }

    @Test
    void loaderSkipsBrokenFilesAndWarnsAboutUnknownCapabilitiesOrWork() {
        PetTaskTypeLoader.Loaded loaded = PetTaskTypeLoader.load(Map.of(
            id("good"), JsonParser.parseString("""
                { "capability": "chiikawa:farmer", "counter": "chiikawa:weed", "icon": "minecraft:short_grass",
                  "amount": 8, "reward": "chiikawa:pet_task/good" }"""),
            id("odd"), JsonParser.parseString("""
                { "capability": "chiikawa:baker", "counter": "chiikawa:bake", "icon": "minecraft:cake",
                  "amount": 8, "reward": "chiikawa:pet_task/odd" }"""),
            id("broken"), JsonParser.parseString("{ \"capability\": \"chiikawa:farmer\" }")
        ), Set.of(FARMER)::contains);

        assertEquals(Set.of(id("good"), id("odd")), loaded.types().keySet());
        assertEquals(1, loaded.errors().size());
        assertEquals(2, loaded.warnings().size());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chiikawa", path);
    }
}
