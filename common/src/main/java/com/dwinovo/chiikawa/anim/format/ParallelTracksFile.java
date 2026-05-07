package com.dwinovo.chiikawa.anim.format;

import com.dwinovo.chiikawa.anim.baked.ParallelTrack;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-pet "parallel tracks" sidecar. Lives at
 * {@code assets/<ns>/parallel/<pet>.json}, named to match the model key.
 *
 * <h2>Schema</h2>
 * Each entry in {@code tracks} is either a string (shorthand) or an object
 * with at least an {@code animation} field:
 *
 * <pre>{@code
 * {
 *   "tracks": [
 *     "blink",                                      // shorthand
 *     { "animation": "breath" },                    // explicit form
 *     { "animation": "tail_wag", "when": "..." }    // future fields tolerated
 *   ]
 * }
 * }</pre>
 *
 * <p>The polymorphic shorthand is intentional: when fields like
 * {@code condition}, {@code speed}, or {@code weight} get added to
 * {@link ParallelTrack}, existing string-shorthand entries keep parsing
 * (they fall back to defaults). Existing object entries with unknown fields
 * are silently ignored, so adding a field is non-breaking on both sides.
 *
 * <p>Animations listed here are looped continuously alongside BASE / sub
 * slots and sampled <em>after</em> them so they win on shared bones —
 * matching the YSM "post-parallel" semantic.
 *
 * <p>Parsing is done by {@link #parse(JsonObject)} rather than direct Gson
 * binding so the polymorphic entry shape doesn't require a custom
 * TypeAdapter registration on the loader's shared {@code Gson} instance.
 */
public final class ParallelTracksFile {

    public List<ParallelTrack> tracks = List.of();

    private ParallelTracksFile() {}

    /** Returns an empty file when {@code root} is null or has no usable {@code tracks} array. */
    public static ParallelTracksFile parse(JsonObject root) {
        ParallelTracksFile out = new ParallelTracksFile();
        if (root == null) return out;
        JsonElement tracksEl = root.get("tracks");
        if (tracksEl == null || !tracksEl.isJsonArray()) return out;

        JsonArray arr = tracksEl.getAsJsonArray();
        List<ParallelTrack> parsed = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            ParallelTrack track = parseEntry(el);
            if (track != null) parsed.add(track);
        }
        out.tracks = List.copyOf(parsed);
        return out;
    }

    /** Returns {@code null} when an entry is malformed (caller skips it). */
    private static ParallelTrack parseEntry(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;

        // Shorthand: "blink" → ParallelTrack("blink")
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            String name = el.getAsString();
            if (name.isBlank()) return null;
            return new ParallelTrack(name);
        }

        // Object form: { "animation": "blink", ... }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            JsonElement animEl = obj.get("animation");
            if (animEl == null || !animEl.isJsonPrimitive() || !animEl.getAsJsonPrimitive().isString()) {
                return null;
            }
            String name = animEl.getAsString();
            if (name.isBlank()) return null;
            return new ParallelTrack(name);
        }

        return null;
    }
}
