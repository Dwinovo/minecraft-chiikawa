package com.dwinovo.chiikawa.entity.brain.personality;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/<namespace>/pet_personality/<entity>.json},
 * where the file id is the pet's entity type id. A file that fails to parse is skipped,
 * so that pet falls back to {@link Personality#DEFAULT}; intent ids that no intent is
 * registered under are only warned about.
 */
public final class PetPersonalityLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_personality";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-personality] ";

    public PetPersonalityLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        PetPersonalities.replaceAll(loaded.personalities());
        Constants.LOG.info(LOG_PREFIX + "loaded {} pet personalities", loaded.personalities().size());
    }

    /**
     * @param files parsed JSON by file id
     * @return the personalities that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, Personality> personalities = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((entityId, json) -> Personality.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(personality -> {
                personalities.put(entityId, personality);
                personality.intentIds().stream()
                    .filter(intent -> PetIntents.get(intent) == null)
                    .sorted()
                    .forEach(intent -> warnings.add(LOG_PREFIX + entityId + " weighs unknown intent " + intent));
            })
            .ifError(error -> errors.add(LOG_PREFIX + entityId + " uses the default personality, failed to parse: "
                + error.message())));
        return new Loaded(personalities, errors, warnings);
    }

    /**
     * @param personalities decoded personalities by entity type id
     * @param errors one message per file that could not be decoded
     * @param warnings one message per unknown intent id
     */
    record Loaded(Map<ResourceLocation, Personality> personalities, List<String> errors, List<String> warnings) {
    }
}
