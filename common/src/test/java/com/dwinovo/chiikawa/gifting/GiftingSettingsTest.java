package com.dwinovo.chiikawa.gifting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GiftingSettingsTest {
    private static final String GOOD = """
        { "cooldown_ticks": 24000,
          "weights": [ { "item": "minecraft:cookie", "weight": 5 }, { "item": "#chiikawa:currency", "weight": 2 } ] }""";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void settingsParseAnItemOrAHashAndATag() {
        GiftingSettings settings = GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(GOOD)).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(24000, settings.cooldownTicks());
        assertEquals(2, settings.weights().size());
        assertTrue(settings.weights().get(0).match().right().isPresent());
        assertTrue(settings.weights().get(1).match().left().isPresent());
        assertEquals(new ResourceLocation("chiikawa", "currency"),
            settings.weights().get(1).match().left().orElseThrow().location());
    }

    @Test
    void settingsEncodeToWhatTheyParseFrom() {
        GiftingSettings settings = GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(GOOD)).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        JsonElement json = GiftingSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(settings, GiftingSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, org.junit.jupiter.api.Assertions::fail));
        assertEquals("#chiikawa:currency", json.getAsJsonObject().getAsJsonArray("weights").get(1)
            .getAsJsonObject().get("item").getAsString());
    }

    @Test
    void settingsThatCannotBeRightAreRejected() {
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("24000", "-1"))).result().isEmpty(), "a cooldown before the beginning of time");
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"weight\": 5", "\"weight\": 0"))).result().isEmpty(), "a present weighing nothing");
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("minecraft:cookie", "minecraft:no_such_thing"))).result().isEmpty());
        assertTrue(GiftingSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{ \"cooldown_ticks\": 1 }")).result().isEmpty(), "no weights at all");
    }

    @Test
    void theLoaderTakesTheOneFileAndNothingElse() {
        ResourceLocation other = new ResourceLocation("chiikawa", "other");
        JsonElement good = JsonParser.parseString(GOOD);

        assertTrue(GiftingSettingsLoader.load(Map.of(GiftingSettingsLoader.FILE, good)).isPresent());
        assertEquals(Optional.empty(), GiftingSettingsLoader.load(Map.of(other, good)), "a file of another name");
        assertEquals(Optional.empty(), GiftingSettingsLoader.load(Map.of()));
    }

    @Test
    void aBrokenFileLeavesNoSettingsRatherThanGuessedOnes() {
        assertEquals(Optional.empty(), GiftingSettingsLoader.load(Map.of(GiftingSettingsLoader.FILE,
            JsonParser.parseString("{ \"cooldown_ticks\": \"soon\" }"))));
    }
}
