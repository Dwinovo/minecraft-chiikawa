package com.dwinovo.chiikawa.whistle;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
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
 * Server data reload listener for {@code data/<namespace>/pet_whistle/<item>.json}. A file
 * that fails to parse is skipped with a message, and its item can no longer be blown: a
 * whistle with broken settings should do nothing, not take the world down with it.
 */
public final class WhistleSettingsLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_whistle";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-whistle] ";

    public WhistleSettingsLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        WhistleSettings.replaceAll(loaded.settings());
        Constants.LOG.info(LOG_PREFIX + "loaded settings for {} whistles", loaded.settings().size());
    }

    /**
     * @param files parsed JSON by file id, which is the id of the item
     * @return the settings that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, WhistleSettings> settings = new HashMap<>();
        List<String> errors = new ArrayList<>();
        files.forEach((id, json) -> {
            DataResult<WhistleSettings> parsed = WhistleSettings.CODEC.parse(JsonOps.INSTANCE, json);
            parsed.result().ifPresent(value -> settings.put(id, value));
            parsed.error().ifPresent(error ->
                errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message()));
        });
        return new Loaded(settings, errors);
    }

    /**
     * @param settings decoded settings by item id
     * @param errors one message per file that could not be decoded
     */
    record Loaded(Map<ResourceLocation, WhistleSettings> settings, List<String> errors) {
    }
}
