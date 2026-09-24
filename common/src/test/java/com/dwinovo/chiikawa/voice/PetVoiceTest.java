package com.dwinovo.chiikawa.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.testing.FixedRandom;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PetVoiceTest {
    private static final String YADA = "voice.chiikawa.chiikawa.hurt.1";
    private static final String SOB = "voice.chiikawa.chiikawa.hurt.2";
    private static final String NNSHO = "voice.chiikawa.chiikawa.idle.1";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void parsesEveryField() {
        PetVoice voice = parse("""
            {
              "lines": {
                "hurt": [ { "text": "voice.chiikawa.chiikawa.hurt.1", "weight": 3 },
                          { "text": "voice.chiikawa.chiikawa.hurt.2", "weight": 1 } ],
                "idle": [ { "text": "voice.chiikawa.chiikawa.idle.1", "weight": 1 } ]
              },
              "chance": { "idle": 0.02 },
              "cooldown_ticks": 200,
              "crowd_limit": 3,
              "talk_ticks": 60,
              "hearing_range": 16.0
            }
            """);

        assertEquals(List.of(YADA, SOB), voice.lines().get(VoiceMoment.HURT).stream().map(PetVoice.Line::text).toList());
        assertEquals(3, voice.lines().get(VoiceMoment.HURT).get(0).weight().asInt());
        assertEquals(0.02F, voice.chance().get(VoiceMoment.IDLE), 1.0E-6F);
        assertEquals(200, voice.cooldownTicks());
        assertEquals(3, voice.crowdLimit());
        assertEquals(60, voice.talkTicks());
        assertEquals(16.0, voice.hearingRange(), 1.0E-9);
    }

    @Test
    void aMomentWithNoLinesPassesInSilenceWithoutRollingAnything() {
        PetVoice voice = new PetVoice(Map.of(VoiceMoment.HURT, List.of(new PetVoice.Line(YADA, 1))), Map.of(),
            200, 3, 60, 16.0);

        // A random source with nothing in it throws if anything is drawn from it.
        assertEquals(Optional.empty(), voice.draw(VoiceMoment.HUNT, FixedRandom.floats()));
    }

    @Test
    void aMomentWithNoChanceListedAlwaysSpeaksAndPicksByWeight() {
        PetVoice voice = new PetVoice(Map.of(VoiceMoment.HURT,
            List.of(new PetVoice.Line(YADA, 3), new PetVoice.Line(SOB, 1))), Map.of(), 200, 3, 60, 16.0);

        assertEquals(Optional.of(YADA), voice.draw(VoiceMoment.HURT, FixedRandom.ints(2)));
        assertEquals(Optional.of(SOB), voice.draw(VoiceMoment.HURT, FixedRandom.ints(3)));
    }

    @Test
    void theChanceDecidesWhetherThePetSpeaksUpAtAll() {
        PetVoice voice = new PetVoice(Map.of(VoiceMoment.IDLE, List.of(new PetVoice.Line(NNSHO, 1))),
            Map.of(VoiceMoment.IDLE, 0.25F), 200, 3, 60, 16.0);

        assertEquals(Optional.empty(), voice.draw(VoiceMoment.IDLE, FixedRandom.floats(0.25F)));
        assertEquals(Optional.of(NNSHO), voice.draw(VoiceMoment.IDLE, FixedRandom.of(new float[] {0.2F}, 0)));
    }

    @Test
    void rejectsWhatCannotBeRight() {
        assertTrue(decode("{ \"lines\": {}, \"cooldown_ticks\": 200 }").isError(), "no crowd limit");
        assertTrue(decode("{ \"lines\": {}, \"crowd_limit\": 3 }").isError(), "no cooldown");
        assertTrue(decode("{ \"lines\": {}, \"cooldown_ticks\": 200, \"crowd_limit\": 0 }").isError());
        assertTrue(decode("{ \"lines\": {}, \"chance\": { \"idle\": 1.5 }, \"cooldown_ticks\": 200, \"crowd_limit\": 3 }").isError());
        assertTrue(decode("{ \"lines\": { \"sneeze\": [] }, \"cooldown_ticks\": 200, \"crowd_limit\": 3 }").isError());
        assertTrue(decode("""
            { "lines": { "hurt": [ { "text": "voice.chiikawa.chiikawa.hurt.1", "weight": 0 } ] },
              "cooldown_ticks": 200, "crowd_limit": 3 }""").isError());
    }

    @Test
    void encodesWhatItParses() {
        PetVoice voice = new PetVoice(Map.of(VoiceMoment.HURT, List.of(new PetVoice.Line(YADA, 2))),
            Map.of(VoiceMoment.IDLE, 0.001F), 400, 2, 60, 16.0);

        JsonElement json = PetVoice.CODEC.encodeStart(JsonOps.INSTANCE, voice).getOrThrow();

        assertEquals(voice, PetVoice.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    private static PetVoice parse(String json) {
        return decode(json).getOrThrow();
    }

    private static DataResult<PetVoice> decode(String json) {
        return PetVoice.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
