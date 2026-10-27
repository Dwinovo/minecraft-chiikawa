package com.dwinovo.chiikawa.entity.brain.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.SurroundingsSettingsData;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** How far a pet looks round itself, and how it tells that its owner is back: the file, and what is made of it. */
class SurroundingsSettingsTest {
    @Test
    void theSettingsTheModShipsReadBackAsTheyWereWrittenAndLookEightBlocksRound() {
        SurroundingsSettings shipped = SurroundingsSettingsData.all().get(SurroundingsSettingsLoader.FILE);
        JsonElement json = SurroundingsSettings.CODEC.encodeStart(JsonOps.INSTANCE, shipped).getOrThrow();

        assertEquals(shipped, SurroundingsSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(8, shipped.radius());
    }

    @Test
    void theFileIsReadWhenItIsThereAndNothingElseIsWhenItIsNot() {
        JsonElement good = JsonParser.parseString("{ \"radius\": 6, \"away_ticks\": 100, \"returned_ticks\": 50, \"settle_ticks\": 20, \"spell_ticks\": 80, \"finished_ticks\": 30 }");

        assertEquals(Optional.of(new SurroundingsSettings(6, 100, 50, 20, 80, 30)),
            SurroundingsSettingsLoader.load(Map.of(SurroundingsSettingsLoader.FILE, good)));
        assertTrue(SurroundingsSettingsLoader.load(Map.of()).isEmpty());
        assertTrue(SurroundingsSettingsLoader.load(Map.of(ResourceLocation.fromNamespaceAndPath("chiikawa", "other"), good)).isEmpty());
    }

    @Test
    void aFileThatIsWrongIsNotGuessedAt() {
        for (String broken : new String[] {"{ \"radius\": 0, \"away_ticks\": 100, \"returned_ticks\": 50, \"settle_ticks\": 20, \"spell_ticks\": 80, \"finished_ticks\": 30 }",
            "{ \"radius\": 6, \"away_ticks\": 100, \"returned_ticks\": 50 }", "{ \"radius\": \"near\", \"away_ticks\": 1, \"returned_ticks\": 1 }"}) {
            assertTrue(SurroundingsSettingsLoader.load(Map.of(SurroundingsSettingsLoader.FILE, JsonParser.parseString(broken))).isEmpty(), broken);
        }
    }
}
