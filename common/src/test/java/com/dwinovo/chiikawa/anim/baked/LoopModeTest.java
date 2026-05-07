package com.dwinovo.chiikawa.anim.baked;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoopModeTest {

    @Test
    void booleanFalseMapsToPlayOnce() {
        assertEquals(LoopMode.PLAY_ONCE, LoopMode.fromBedrockJson(new JsonPrimitive(false)));
    }

    @Test
    void booleanTrueMapsToLoop() {
        assertEquals(LoopMode.LOOP, LoopMode.fromBedrockJson(new JsonPrimitive(true)));
    }

    @Test
    void holdOnLastFrameStringMapsToHoldEnum() {
        assertEquals(LoopMode.HOLD_ON_LAST_FRAME,
                LoopMode.fromBedrockJson(new JsonPrimitive("hold_on_last_frame")));
    }

    @Test
    void stringifiedBooleansFromLegacyExportersAreTolerated() {
        assertEquals(LoopMode.LOOP, LoopMode.fromBedrockJson(new JsonPrimitive("true")));
        assertEquals(LoopMode.PLAY_ONCE, LoopMode.fromBedrockJson(new JsonPrimitive("false")));
    }

    @Test
    void unknownStringFallsBackToPlayOnce() {
        assertEquals(LoopMode.PLAY_ONCE, LoopMode.fromBedrockJson(new JsonPrimitive("???")));
    }

    @Test
    void nullAndMissingFallBackToPlayOnce() {
        assertEquals(LoopMode.PLAY_ONCE, LoopMode.fromBedrockJson(null));
        assertEquals(LoopMode.PLAY_ONCE, LoopMode.fromBedrockJson(JsonNull.INSTANCE));
    }

    @Test
    void parsesFromAnimationJsonObjectExtraction() {
        // Sanity check end-to-end with a JsonObject lookup like AnimationBaker uses.
        JsonObject obj = JsonParser.parseString(
                "{ \"loop\": \"hold_on_last_frame\" }").getAsJsonObject();
        assertEquals(LoopMode.HOLD_ON_LAST_FRAME, LoopMode.fromBedrockJson(obj.get("loop")));
    }
}
