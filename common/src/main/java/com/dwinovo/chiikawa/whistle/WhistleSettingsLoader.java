package com.dwinovo.chiikawa.whistle;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/<namespace>/pet_whistle/<item>.json}. A file
 * that fails to parse is skipped with a message, and its item can no longer be blown: a
 * whistle with broken settings should do nothing, not take the world down with it.
 */
public final class WhistleSettingsLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final String DIRECTORY = "pet_whistle";
    /** Id for loaders that register reload listeners by id. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-whistle] ";

    public WhistleSettingsLoader() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        WhistleSettings.replaceAll(loaded.settings());
        Constants.LOG.info(LOG_PREFIX + "loaded settings for {} whistles", loaded.settings().size());
    }

    /**
     * @param files parsed JSON by file id, which is the id of the item
     * @return the settings that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<Identifier, JsonElement> files) {
        Map<Identifier, WhistleSettings> settings = new HashMap<>();
        List<String> errors = new ArrayList<>();
        files.forEach((id, json) -> WhistleSettings.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(parsed -> settings.put(id, parsed))
            .ifError(error -> errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message())));
        return new Loaded(settings, errors);
    }

    /**
     * @param settings decoded settings by item id
     * @param errors one message per file that could not be decoded
     */
    record Loaded(Map<Identifier, WhistleSettings> settings, List<String> errors) {
    }
}
