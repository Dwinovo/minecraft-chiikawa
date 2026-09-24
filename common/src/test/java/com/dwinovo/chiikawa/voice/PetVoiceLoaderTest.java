package com.dwinovo.chiikawa.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PetVoiceLoaderTest {
    private static final ResourceLocation CHIIKAWA = ResourceLocation.fromNamespaceAndPath("chiikawa", "chiikawa");
    private static final ResourceLocation USAGI = ResourceLocation.fromNamespaceAndPath("chiikawa", "usagi");
    private static final ResourceLocation RAKKO = ResourceLocation.fromNamespaceAndPath("chiikawa", "rakko");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void clearLoaded() {
        PetVoices.replaceAll(Map.of());
    }

    @Test
    void aBrokenFileLeavesThatPetSilentAndTheRestTalking() {
        PetVoiceLoader.Loaded loaded = PetVoiceLoader.load(Map.of(
            CHIIKAWA, JsonParser.parseString("""
                { "lines": { "hurt": [ { "text": "voice.chiikawa.chiikawa.hurt.1", "weight": 1 } ] },
                  "cooldown_ticks": 200, "crowd_limit": 3, "talk_ticks": 60, "hearing_range": 16.0 }"""),
            USAGI, JsonParser.parseString("{ \"lines\": { \"cry\": [] }, \"cooldown_ticks\": 200, \"crowd_limit\": 3 }")));
        PetVoices.replaceAll(loaded.voices());

        assertEquals(Set.of(CHIIKAWA), loaded.voices().keySet());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains(USAGI.toString()), loaded.errors()::toString);
        assertEquals(1, PetVoices.get(CHIIKAWA).lines().get(VoiceMoment.HURT).size());
        assertSame(PetVoice.SILENT, PetVoices.get(USAGI));
        assertSame(PetVoice.SILENT, PetVoices.get(RAKKO), "a pet with no file says nothing");
    }
}
