package com.dwinovo.chiikawa.entity.brain.constraint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.AnchorSettingsData;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AnchorSettingsTest {
    private static final String GOOD = """
        { "follow": { "start": 7, "arrive": 2, "reach": 12, "leash": 16, "teleport": 20 },
          "free": { "reach": 13, "leash": 20 },
          "wild": { "reach": 24, "leash": 32 } }""";

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    @Test
    void settingsParseTheThreeAreas() {
        AnchorSettings settings = AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json(GOOD)).getOrThrow();

        assertEquals(new AnchorSettings.Follow(7.0, 2, 12.0, 16.0, 20.0), settings.follow());
        assertEquals(new AnchorSettings.Area(13.0, 20.0), settings.free());
        assertEquals(new AnchorSettings.Area(24.0, 32.0), settings.wild());
    }

    @Test
    void settingsEncodeToWhatTheyParseFrom() {
        AnchorSettings settings = AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json(GOOD)).getOrThrow();

        JsonElement encoded = AnchorSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();

        assertEquals(settings, AnchorSettings.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void distancesThatCannotBeRightAreRejected() {
        assertTrue(AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json(GOOD.replace("\"leash\": 20", "\"leash\": 10"))).isError(),
            "a leash shorter than the reach");
        assertTrue(AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json(GOOD.replace("\"teleport\": 20", "\"teleport\": 14"))).isError(),
            "a pet fetched before it is let off the leash");
        assertTrue(AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json(GOOD.replace("\"start\": 7", "\"start\": 15"))).isError(),
            "a pet that starts walking back after its owner is out of reach");
        assertTrue(AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json(GOOD.replace("\"reach\": 13", "\"reach\": -1"))).isError());
        assertTrue(AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json("{ \"free\": { \"reach\": 1, \"leash\": 2 } }")).isError(),
            "an area missing");
    }

    @Test
    void theLoaderTakesTheOneFileAndNothingElse() {
        ResourceLocation other = new ResourceLocation("chiikawa", "other");

        assertTrue(AnchorSettingsLoader.load(Map.of(AnchorSettingsLoader.FILE, json(GOOD))).isPresent());
        assertEquals(Optional.empty(), AnchorSettingsLoader.load(Map.of(other, json(GOOD))), "a file of another name");
        assertEquals(Optional.empty(), AnchorSettingsLoader.load(Map.of()));
    }

    @Test
    void aBrokenFileLeavesNoSettingsRatherThanGuessedOnes() {
        assertEquals(Optional.empty(), AnchorSettingsLoader.load(Map.of(AnchorSettingsLoader.FILE,
            json("{ \"follow\": 7 }"))));
    }

    @Test
    void theGeneratedSettingsAreTheSizesTheTestsAssume() {
        assertEquals(TestAnchors.SETTINGS, AnchorSettingsData.all().get(AnchorSettingsLoader.FILE));
    }
}
