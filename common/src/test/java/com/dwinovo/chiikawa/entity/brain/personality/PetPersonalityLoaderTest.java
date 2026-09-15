package com.dwinovo.chiikawa.entity.brain.personality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PetPersonalityLoaderTest {
    private static final ResourceLocation CHIIKAWA = ResourceLocation.fromNamespaceAndPath("chiikawa", "chiikawa");
    private static final ResourceLocation USAGI = ResourceLocation.fromNamespaceAndPath("chiikawa", "usagi");
    private static final ResourceLocation HACHIWARE = ResourceLocation.fromNamespaceAndPath("chiikawa", "hachiware");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void clearLoaded() {
        PetPersonalities.replaceAll(Map.of());
    }

    @Test
    void unparsableFileFallsBackToTheDefaultPersonality() {
        PetPersonalityLoader.Loaded loaded = PetPersonalityLoader.load(Map.of(
            CHIIKAWA, JsonParser.parseString("{ \"intent_multipliers\": { \"chiikawa:harvest\": 1.2 } }"),
            USAGI, JsonParser.parseString("{ \"randomness\": \"very\" }")));
        PetPersonalities.replaceAll(loaded.personalities());

        assertEquals(Set.of(CHIIKAWA), loaded.personalities().keySet());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains(USAGI.toString()), loaded.errors()::toString);
        assertTrue(loaded.warnings().isEmpty(), loaded.warnings()::toString);

        assertEquals(1.2F, PetPersonalities.get(CHIIKAWA).multiplier(PetIntents.HARVEST, DayPhase.DAY), 1.0E-6F);
        assertSame(Personality.DEFAULT, PetPersonalities.get(USAGI));
        assertSame(Personality.DEFAULT, PetPersonalities.get(HACHIWARE));
    }

    @Test
    void warnsAboutUnknownIntentIdsButKeepsThePersonality() {
        PetPersonalityLoader.Loaded loaded = PetPersonalityLoader.load(Map.of(HACHIWARE, JsonParser.parseString("""
            {
              "intent_multipliers": { "chiikawa:play_music": 1.3, "chiikawa:sing": 2.0 },
              "routine": { "EVENING": { "chiikawa:dance": 1.5 } }
            }
            """)));

        assertEquals(Set.of(HACHIWARE), loaded.personalities().keySet());
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertEquals(2, loaded.warnings().size(), loaded.warnings()::toString);
        assertTrue(loaded.warnings().get(0).contains("chiikawa:dance"), loaded.warnings()::toString);
        assertTrue(loaded.warnings().get(1).contains("chiikawa:sing"), loaded.warnings()::toString);
    }
}
