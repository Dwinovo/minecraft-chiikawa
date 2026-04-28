package com.dwinovo.chiikawa.anim.compile;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.baked.BakedAnimation;
import com.dwinovo.chiikawa.anim.baked.BakedBoneChannel;
import com.dwinovo.chiikawa.anim.baked.BakedModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles Bedrock {@code .animation.json} files into immutable
 * {@link BakedAnimation}s, expressed against a specific {@link BakedModel}'s
 * bone layout.
 *
 * <p>The animation file schema is heterogeneous (a channel can be a constant
 * vector, a keyframe map, with shorthand or pre/post forms), so we walk the
 * raw {@code JsonObject} rather than introducing a Gson POJO layer that would
 * otherwise be mostly {@code JsonElement} fields anyway.
 *
 * <p>Phase 2 scope: numeric values only. Molang-string vector components are
 * silently treated as {@code 0} — Phase 3 will replace this fallback with a
 * compiled {@code MolangNode} slot.
 */
public final class AnimationBaker {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final float[] EMPTY_FLOATS = new float[0];
    private static final byte[]  EMPTY_BYTES  = new byte[0];

    private AnimationBaker() {}

    /** Bakes every animation in {@code root.animations} against {@code model}. */
    public static Map<String, BakedAnimation> bake(JsonObject root, BakedModel model) {
        Map<String, BakedAnimation> out = new HashMap<>();
        JsonElement animsEl = root.get("animations");
        if (animsEl == null || !animsEl.isJsonObject()) return out;

        for (Map.Entry<String, JsonElement> entry : animsEl.getAsJsonObject().entrySet()) {
            String name = entry.getKey();
            if (!entry.getValue().isJsonObject()) continue;
            try {
                BakedAnimation baked = bakeOne(name, entry.getValue().getAsJsonObject(), model);
                out.put(name, baked);
            } catch (Exception ex) {
                Constants.LOG.error("[chiikawa-anim] failed to bake animation {}: {}", name, ex.toString());
            }
        }
        return out;
    }

    private static BakedAnimation bakeOne(String name, JsonObject obj, BakedModel model) {
        boolean loop = obj.has("loop") && asBoolean(obj.get("loop"));
        float duration = obj.has("animation_length") ? obj.get("animation_length").getAsFloat() : 0f;

        List<BakedBoneChannel> channels = new ArrayList<>();
        JsonElement bonesEl = obj.get("bones");
        if (bonesEl != null && bonesEl.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : bonesEl.getAsJsonObject().entrySet()) {
                Integer boneIdx = model.boneIndex.get(e.getKey());
                if (boneIdx == null) continue; // animation references a bone the model doesn't have
                if (!e.getValue().isJsonObject()) continue;
                addBoneChannels(boneIdx, e.getValue().getAsJsonObject(), channels);
            }
        }

        if (duration <= 0f) {
            duration = computeDuration(channels);
        }
        return new BakedAnimation(name, duration, loop, channels.toArray(new BakedBoneChannel[0]));
    }

    private static void addBoneChannels(int boneIdx, JsonObject boneAnim, List<BakedBoneChannel> out) {
        JsonElement rot = boneAnim.get("rotation");
        if (rot != null) bakeChannel(boneIdx, BakedBoneChannel.TYPE_ROTATION, rot, out);
        JsonElement pos = boneAnim.get("position");
        if (pos != null) bakeChannel(boneIdx, BakedBoneChannel.TYPE_POSITION, pos, out);
        JsonElement scale = boneAnim.get("scale");
        if (scale != null) bakeChannel(boneIdx, BakedBoneChannel.TYPE_SCALE, scale, out);
    }

    private static void bakeChannel(int boneIdx, byte type, JsonElement data, List<BakedBoneChannel> out) {
        if (data.isJsonArray()) {
            // Bare-array shorthand ("rotation": [x, y, z]).
            float[] v = readVec3(data);
            applyMirrorAndUnits(v, type);
            out.add(new BakedBoneChannel(boneIdx, type, true, EMPTY_FLOATS, v, EMPTY_BYTES));
            return;
        }
        if (!data.isJsonObject()) return;
        JsonObject obj = data.getAsJsonObject();

        // Detect form: shorthand "vector" with no time keys = constant channel.
        if (obj.has("vector") && !hasTimeKeys(obj)) {
            float[] v = readVec3(obj.get("vector"));
            applyMirrorAndUnits(v, type);
            out.add(new BakedBoneChannel(boneIdx, type, true, EMPTY_FLOATS, v, EMPTY_BYTES));
            return;
        }

        // Keyframed form: gather all numeric keys, sort by time.
        List<TimedKey> keys = new ArrayList<>(obj.size());
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            float t;
            try {
                t = Float.parseFloat(e.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            if (!e.getValue().isJsonObject() && !e.getValue().isJsonArray()) continue;
            keys.add(new TimedKey(t, e.getValue()));
        }
        if (keys.isEmpty()) return;
        keys.sort(Comparator.comparingDouble(k -> k.time));

        int n = keys.size();
        float[] times = new float[n];
        float[] values = new float[n * 6];
        byte[] lerpModes = new byte[n];
        for (int i = 0; i < n; i++) {
            TimedKey key = keys.get(i);
            times[i] = key.time;

            float[] pre = new float[3];
            float[] post = new float[3];
            byte lerp = BakedBoneChannel.LERP_LINEAR;

            if (key.value.isJsonArray()) {
                // Shorthand: time -> [x, y, z]
                float[] v = readVec3(key.value);
                System.arraycopy(v, 0, pre, 0, 3);
                System.arraycopy(v, 0, post, 0, 3);
            } else {
                JsonObject kf = key.value.getAsJsonObject();
                if (kf.has("vector")) {
                    // Shorthand: time -> { "vector": [...] }
                    float[] v = readVec3(kf.get("vector"));
                    System.arraycopy(v, 0, pre, 0, 3);
                    System.arraycopy(v, 0, post, 0, 3);
                } else {
                    // Expanded: time -> { "pre"?: ..., "post"?: ..., "lerp_mode"?: ... }
                    JsonElement preEl = kf.get("pre");
                    JsonElement postEl = kf.get("post");
                    if (preEl != null) pre = readKfPart(preEl);
                    if (postEl != null) post = readKfPart(postEl);
                    // If only one side specified, mirror onto the other.
                    if (preEl == null && postEl != null) System.arraycopy(post, 0, pre, 0, 3);
                    if (postEl == null && preEl != null) System.arraycopy(pre, 0, post, 0, 3);
                }
                if (kf.has("lerp_mode")) {
                    JsonElement lm = kf.get("lerp_mode");
                    if (lm.isJsonPrimitive() && lm.getAsJsonPrimitive().isString()) {
                        lerp = parseLerpMode(lm.getAsString());
                    }
                }
            }

            applyMirrorAndUnits(pre, type);
            applyMirrorAndUnits(post, type);

            int base = i * 6;
            values[base]     = pre[0];
            values[base + 1] = pre[1];
            values[base + 2] = pre[2];
            values[base + 3] = post[0];
            values[base + 4] = post[1];
            values[base + 5] = post[2];
            lerpModes[i] = lerp;
        }

        out.add(new BakedBoneChannel(boneIdx, type, false, times, values, lerpModes));
    }

    private static boolean hasTimeKeys(JsonObject obj) {
        for (String key : obj.keySet()) {
            try {
                Float.parseFloat(key);
                return true;
            } catch (NumberFormatException ignored) {
                // not a numeric key
            }
        }
        return false;
    }

    private static float[] readVec3(JsonElement el) {
        float[] out = new float[3];
        if (el == null || !el.isJsonArray()) return out;
        JsonArray arr = el.getAsJsonArray();
        int n = Math.min(3, arr.size());
        for (int i = 0; i < n; i++) {
            JsonElement c = arr.get(i);
            if (!c.isJsonPrimitive()) continue;
            JsonPrimitive p = c.getAsJsonPrimitive();
            if (p.isNumber()) {
                out[i] = p.getAsFloat();
            } else if (p.isString()) {
                String s = p.getAsString();
                try {
                    out[i] = Float.parseFloat(s);
                } catch (NumberFormatException ignored) {
                    // Molang expression (Phase 3) — fall back to 0.
                }
            }
        }
        return out;
    }

    /** Resolves {@code "pre"}/{@code "post"} payload into a vec3. */
    private static float[] readKfPart(JsonElement kfPart) {
        if (kfPart == null) return new float[3];
        if (kfPart.isJsonArray()) return readVec3(kfPart);
        if (kfPart.isJsonObject()) {
            JsonObject o = kfPart.getAsJsonObject();
            if (o.has("vector")) return readVec3(o.get("vector"));
        }
        return new float[3];
    }

    private static byte parseLerpMode(String s) {
        if (s == null) return BakedBoneChannel.LERP_LINEAR;
        return switch (s) {
            case "catmullrom" -> BakedBoneChannel.LERP_CATMULL_ROM;
            case "step" -> BakedBoneChannel.LERP_STEP;
            default -> BakedBoneChannel.LERP_LINEAR;
        };
    }

    private static float computeDuration(List<BakedBoneChannel> channels) {
        float max = 0f;
        for (BakedBoneChannel ch : channels) {
            if (ch.times.length > 0) {
                max = Math.max(max, ch.times[ch.times.length - 1]);
            }
        }
        return max;
    }

    /**
     * In-place mirror + unit conversion to match the bake-time X flip applied
     * to {@link com.dwinovo.chiikawa.anim.baked.BakedModel} bones (see
     * {@link ModelBaker}). Animation values come from the un-flipped Bedrock
     * frame, so we apply the same mirror here so that combining a sampled
     * delta with a baked rest pose produces a consistent local frame.
     *
     * <ul>
     *   <li>{@code rotation}: convert to radians, negate X and Y, keep Z
     *       (matches {@code ModelBaker.bakeCube}'s rest-rotation flip).</li>
     *   <li>{@code position}: negate X, keep Y and Z (matches the pivot.x
     *       negation).</li>
     *   <li>{@code scale}: unchanged — scale is mirror-symmetric.</li>
     * </ul>
     */
    private static void applyMirrorAndUnits(float[] v, byte type) {
        if (type == BakedBoneChannel.TYPE_ROTATION) {
            v[0] = -v[0] * DEG_TO_RAD;
            v[1] = -v[1] * DEG_TO_RAD;
            v[2] =  v[2] * DEG_TO_RAD;
        } else if (type == BakedBoneChannel.TYPE_POSITION) {
            v[0] = -v[0];
        }
    }

    private static boolean asBoolean(JsonElement el) {
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isString()) return Boolean.parseBoolean(p.getAsString());
        }
        return false;
    }

    private record TimedKey(float time, JsonElement value) {}
}
