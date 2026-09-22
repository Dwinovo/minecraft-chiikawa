package com.dwinovo.chiikawa.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.LaborBoardLevelData;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BoardLevelsTest {
    private static final BoardLevels LEVELS = new BoardLevels(List.of(
        new BoardLevels.Level(3, 0), new BoardLevels.Level(4, 16), new BoardLevels.Level(5, 32)));

    @Test
    void aBoardStartsAtTheFirstLevelAndStopsAtTheTop() {
        assertEquals(3, LEVELS.top());
        assertEquals(BoardLevels.FIRST_LEVEL, LEVELS.clamp(0));
        assertEquals(LEVELS.top(), LEVELS.clamp(LEVELS.top() + 4), "a save from a pack with more levels");
    }

    @Test
    void eachLevelHasItsOwnSlipsAndPrice() {
        assertEquals(List.of(3, 4, 5), List.of(LEVELS.slipsAt(1), LEVELS.slipsAt(2), LEVELS.slipsAt(3)));
        assertEquals(16, LEVELS.priceAfter(1));
        assertEquals(32, LEVELS.priceAfter(2));
        assertEquals(0, LEVELS.priceAfter(LEVELS.top()), "nothing left to sell at the top");
    }

    @Test
    void aPackWritesItsLevelsAndTheFirstNeedsNoPrice() {
        BoardLevels parsed = parse("""
            { "levels": [ { "slips": 2 }, { "slips": 6, "price": 10 } ] }""");

        assertEquals(2, parsed.top());
        assertEquals(6, parsed.slipsAt(2));
        assertEquals(10, parsed.priceAfter(BoardLevels.FIRST_LEVEL));
    }

    @Test
    void aLevelPastTheFirstWithoutAPriceIsRefused() {
        assertFalse(BoardLevels.CODEC.parse(JsonOps.INSTANCE, json("""
            { "levels": [ { "slips": 3 }, { "slips": 4 } ] }""")).isSuccess(),
            "a free level reads as the top one: the screen would say there is nothing left to buy");
    }

    @Test
    void aBoardWithNoLevelsOrTooManySlipsIsRefused() {
        assertFalse(BoardLevels.CODEC.parse(JsonOps.INSTANCE, json("""
            { "levels": [] }""")).isSuccess());
        assertFalse(BoardLevels.CODEC.parse(JsonOps.INSTANCE, json(
            "{ \"levels\": [ { \"slips\": " + (BoardLevels.MOST_SLIPS + 1) + " } ] }")).isSuccess(),
            "more slips than the plates sent to players have bits for");
    }

    @Test
    void withoutItsFileABoardPutsNothingUpAndSaysWhy() {
        BoardLevelsLoader.Loaded loaded = BoardLevelsLoader.load(Map.of());

        assertEquals(BoardLevels.NONE, loaded.levels());
        assertEquals(0, loaded.levels().slipsAt(BoardLevels.FIRST_LEVEL));
        assertEquals(0, loaded.levels().priceAfter(BoardLevels.FIRST_LEVEL));
        assertEquals(1, loaded.errors().size());
    }

    @Test
    void aBrokenFileIsTheSameAsNone() {
        BoardLevelsLoader.Loaded loaded = BoardLevelsLoader.load(Map.of(BoardLevelsLoader.FILE, json("""
            { "levels": "three" }""")));

        assertEquals(BoardLevels.NONE, loaded.levels());
        assertEquals(1, loaded.errors().size());
    }

    @Test
    void anotherFileBesideTheLevelsIsNotReadAndSaysSo() {
        BoardLevelsLoader.Loaded loaded = BoardLevelsLoader.load(Map.of(
            BoardLevelsLoader.FILE, json("{ \"levels\": [ { \"slips\": 3 } ] }"),
            ResourceLocation.fromNamespaceAndPath("mypack", "levels"), json("{ \"levels\": [ { \"slips\": 9 } ] }")));

        assertEquals(3, loaded.levels().slipsAt(BoardLevels.FIRST_LEVEL));
        assertTrue(loaded.errors().isEmpty());
        assertEquals(1, loaded.warnings().size());
    }

    @Test
    void theModsOwnLevelsSurviveBeingWrittenOutAndReadBack() {
        JsonElement written = BoardLevels.CODEC.encodeStart(JsonOps.INSTANCE, LaborBoardLevelData.LEVELS).getOrThrow();

        assertEquals(LaborBoardLevelData.LEVELS, BoardLevels.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
        assertFalse(written.getAsJsonObject().getAsJsonArray("levels").get(0).getAsJsonObject().has("price"),
            "the first level is written without a price nobody pays");
    }

    private static BoardLevels parse(String text) {
        return BoardLevels.CODEC.parse(JsonOps.INSTANCE, json(text)).getOrThrow();
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }
}
