package com.dwinovo.chiikawa.entity.brain.personality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PersonalityTest {
    private static final ResourceLocation WANDER = new ResourceLocation("chiikawa", "wander");
    private static final ResourceLocation HARVEST = new ResourceLocation("chiikawa", "harvest");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void parsesEveryField() {
        Personality personality = parse("""
            {
              "intent_multipliers": { "chiikawa:harvest": 1.2, "chiikawa:wander": 0.5 },
              "routine": { "NIGHT": { "chiikawa:wander": 3.0 } },
              "randomness": 0.1,
              "wild_tools": [
                { "item": "minecraft:wooden_hoe", "weight": 6 },
                { "item": "minecraft:air", "weight": 1 }
              ]
            }
            """);

        assertEquals(1.2F, personality.multiplier(HARVEST, DayPhase.DAY), 1.0E-6F);
        assertEquals(0.5F, personality.multiplier(WANDER, DayPhase.DAY), 1.0E-6F);
        assertEquals(1.5F, personality.multiplier(WANDER, DayPhase.NIGHT), 1.0E-6F);
        assertEquals(0.1F, personality.randomness(), 1.0E-6F);
        assertEquals(List.of(Items.WOODEN_HOE, Items.AIR), personality.wildTools().stream().map(Personality.WeightedItem::item).toList());
        assertEquals(Set.of(HARVEST, WANDER), personality.intentIds());
    }

    @Test
    void omittedFieldsFallBackToTheDefaultPersonality() {
        Personality personality = parse("{}");

        assertEquals(Personality.DEFAULT, personality);
        assertEquals(1.0F, personality.multiplier(HARVEST, DayPhase.NIGHT));
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertTrue(decode("{ \"intent_multipliers\": { \"chiikawa:wander\": -1 } }").error().isPresent());
        assertTrue(decode("{ \"randomness\": 1.5 }").error().isPresent());
        assertTrue(decode("{ \"wild_tools\": [ { \"item\": \"minecraft:stone_hoe\", \"weight\": 0 } ] }").error().isPresent());
        assertTrue(decode("{ \"wild_tools\": [ { \"item\": \"minecraft:no_such_item\", \"weight\": 1 } ] }").error().isPresent());
        assertTrue(decode("{ \"routine\": { \"NOON\": {} } }").error().isPresent());
    }

    @Test
    void encodesWhatItParses() {
        Personality personality = new Personality(Map.of(HARVEST, 1.3F), Map.of(DayPhase.MORNING, Map.of(HARVEST, 1.2F)),
            0.05F, List.of(new Personality.WeightedItem(Items.STONE_SWORD, 4)),
            List.of(new Personality.WeightedItem(Items.COOKIE, 2)), IdleHabits.DEFAULT);

        JsonElement json = Personality.CODEC.encodeStart(JsonOps.INSTANCE, personality).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
        Personality decoded = Personality.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(personality.intentMultipliers(), decoded.intentMultipliers());
        assertEquals(personality.routine(), decoded.routine());
        assertEquals(personality.randomness(), decoded.randomness());
        assertEquals(Items.STONE_SWORD, decoded.wildTools().get(0).item());
        assertEquals(4, decoded.wildTools().get(0).weight().asInt());
    }

    @Test
    void drawsWildToolsByWeight() {
        Personality personality = new Personality(Map.of(), Map.of(), 0.0F, List.of(
            new Personality.WeightedItem(Items.WOODEN_HOE, 6),
            new Personality.WeightedItem(Items.AIR, 1),
            new Personality.WeightedItem(Items.WOODEN_SWORD, 3)), List.of(), IdleHabits.DEFAULT);

        // The draw picks an index below the total weight of 10.
        assertEquals(Items.WOODEN_HOE, personality.drawWildTool(FixedRandom.ints(0)).getItem());
        assertEquals(Items.WOODEN_HOE, personality.drawWildTool(FixedRandom.ints(5)).getItem());
        assertTrue(personality.drawWildTool(FixedRandom.ints(6)).isEmpty());
        assertEquals(Items.WOODEN_SWORD, personality.drawWildTool(FixedRandom.ints(7)).getItem());
        assertEquals(Items.WOODEN_SWORD, personality.drawWildTool(FixedRandom.ints(9)).getItem());
    }

    @Test
    void aPersonalityWithoutWildToolsSpawnsEmptyHanded() {
        assertTrue(Personality.DEFAULT.drawWildTool(FixedRandom.ints(0)).isEmpty());
    }

    @Test
    void idleHabitsSayHowOftenHowNearAndHowLong() {
        IdleHabits idle = parse("""
            {
              "idle": {
                "look_at_player": { "weight": 5, "range": 10, "ticks": { "min_inclusive": 80, "max_inclusive": 140 } },
                "stroll": 0,
                "rest": { "weight": 4, "ticks": { "min_inclusive": 120, "max_inclusive": 120 } }
              }
            }
            """).idle();

        assertEquals(5, idle.lookAtPlayer().weight());
        assertEquals(10.0F, idle.lookAtPlayer().range());
        assertEquals(80, (int) idle.lookAtPlayer().ticks().minInclusive());
        assertEquals(140, (int) idle.lookAtPlayer().ticks().maxInclusive());
        assertEquals(0, idle.stroll(), "a weight of 0 is a pet that never strolls off");
        assertEquals(4, idle.rest().weight());
        assertEquals(120, (int) idle.rest().ticks().maxInclusive());
        // A habit left out is the one every pet has.
        assertEquals(IdleHabits.DEFAULT.lookAtCreature(), idle.lookAtCreature());
    }

    @Test
    void idleHabitsStayWithinWhatAPetCanDo() {
        String ticks = "\"ticks\": { \"min_inclusive\": 40, \"max_inclusive\": 40 }";
        // Farther than a pet sees who is around it.
        assertTrue(decode("{ \"idle\": { \"look_at_player\": { \"weight\": 1, \"range\": 17, " + ticks + " } } }").error().isPresent());
        assertTrue(decode("{ \"idle\": { \"look_at_player\": { \"weight\": -1, \"range\": 5, " + ticks + " } } }").error().isPresent());
        assertTrue(decode("{ \"idle\": { \"rest\": { \"weight\": 1, \"ticks\": { \"min_inclusive\": 0, \"max_inclusive\": 10 } } } }").error().isPresent());
        assertTrue(decode("{ \"idle\": { \"rest\": { \"weight\": 1, \"ticks\": { \"min_inclusive\": 10, \"max_inclusive\": "
            + (IdleHabits.LONGEST_TICKS + 1) + " } } } }").error().isPresent());
        assertTrue(decode("{ \"idle\": { \"stroll\": -1 } }").error().isPresent());
        // Shortest longer than longest.
        assertTrue(decode("{ \"idle\": { \"rest\": { \"weight\": 1, \"ticks\": { \"min_inclusive\": 60, \"max_inclusive\": 30 } } } }").error().isPresent());
    }

    private static Personality parse(String json) {
        return decode(json).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
    }

    private static com.mojang.serialization.DataResult<Personality> decode(String json) {
        return Personality.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
