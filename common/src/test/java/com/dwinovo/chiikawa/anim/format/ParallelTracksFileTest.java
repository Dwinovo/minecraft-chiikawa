package com.dwinovo.chiikawa.anim.format;

import com.dwinovo.chiikawa.anim.baked.ParallelTrack;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelTracksFileTest {

    private static ParallelTracksFile parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return ParallelTracksFile.parse(root);
    }

    @Test
    void parsesShorthandStringEntries() {
        ParallelTracksFile file = parse("{ \"tracks\": [\"blink\", \"breath\"] }");
        assertEquals(2, file.tracks.size());
        assertEquals("blink", file.tracks.get(0).animation());
        assertEquals("breath", file.tracks.get(1).animation());
    }

    @Test
    void parsesObjectEntriesWithAnimationField() {
        ParallelTracksFile file = parse(
                "{ \"tracks\": [{\"animation\": \"blink\"}, {\"animation\": \"breath\"}] }");
        assertEquals(2, file.tracks.size());
        assertEquals("blink", file.tracks.get(0).animation());
    }

    @Test
    void mixesShorthandAndObjectEntries() {
        ParallelTracksFile file = parse(
                "{ \"tracks\": [\"blink\", {\"animation\": \"tail_wag\"}] }");
        assertEquals(2, file.tracks.size());
        assertEquals("blink", file.tracks.get(0).animation());
        assertEquals("tail_wag", file.tracks.get(1).animation());
    }

    @Test
    void unknownObjectFieldsAreIgnoredForForwardCompat() {
        // When future fields like "when" / "speed" / "weight" appear in user files
        // before the parser learns them, the entry must still parse cleanly.
        ParallelTracksFile file = parse(
                "{ \"tracks\": [{\"animation\": \"tail_wag\", \"when\": \"v.mood>0.5\", \"speed\": 1.5}] }");
        assertEquals(1, file.tracks.size());
        assertEquals("tail_wag", file.tracks.get(0).animation());
    }

    @Test
    void malformedEntriesAreSkippedNotFatal() {
        // null entry, empty string, object with no animation field — all dropped silently
        ParallelTracksFile file = parse(
                "{ \"tracks\": [\"\", null, {\"foo\": \"bar\"}, \"blink\"] }");
        assertEquals(1, file.tracks.size());
        assertEquals("blink", file.tracks.get(0).animation());
    }

    @Test
    void missingTracksFieldGivesEmptyList() {
        ParallelTracksFile file = parse("{}");
        assertTrue(file.tracks.isEmpty());
    }

    @Test
    void parsedListIsImmutable() {
        ParallelTracksFile file = parse("{ \"tracks\": [\"blink\"] }");
        // List.copyOf returns an immutable list — same reference contract as elsewhere.
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> file.tracks.add(new ParallelTrack("late")));
    }
}
