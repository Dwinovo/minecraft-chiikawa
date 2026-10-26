package com.dwinovo.chiikawa.entity.brain.personality;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettingsLoader;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/<namespace>/pet_personality/<entity>.json},
 * where the file id is the pet's entity type id. A file that fails to parse is skipped,
 * so that pet falls back to {@link Personality#DEFAULT}; errand ids that no pack has a
 * file for are only warned about.
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
        Loaded loaded = load(files, ErrandSettingsLoader.errandIds(manager));
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        PetPersonalities.replaceAll(loaded.personalities());
        Constants.LOG.info(LOG_PREFIX + "loaded {} pet personalities", loaded.personalities().size());
    }

    /**
     * @param files parsed JSON by file id
     * @param errands the ids of the errands the packs have files for, see {@link ErrandSettingsLoader#errandIds}
     * @return the personalities that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files, Set<ResourceLocation> errands) {
        Map<ResourceLocation, Personality> personalities = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((entityId, json) -> Personality.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(personality -> {
                personalities.put(entityId, personality);
                personality.errandIds().stream()
                    .filter(errand -> !errands.contains(errand))
                    .sorted()
                    .forEach(errand -> warnings.add(LOG_PREFIX + entityId + " weighs unknown errand " + errand));
            })
            .ifError(error -> errors.add(LOG_PREFIX + entityId + " uses the default personality, failed to parse: "
                + error.message())));
        return new Loaded(personalities, errors, warnings);
    }

    /**
     * @param personalities decoded personalities by entity type id
     * @param errors one message per file that could not be decoded
     * @param warnings one message per unknown errand id
     */
    record Loaded(Map<ResourceLocation, Personality> personalities, List<String> errors, List<String> warnings) {
    }
}
