package com.dwinovo.chiikawa.voice;

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
 * Server data reload listener for {@code data/<namespace>/pet_voice/<entity>.json}, where
 * the file id is the pet's entity type id. A file that fails to parse is skipped, so that
 * pet says nothing until it is fixed.
 */
public final class PetVoiceLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final String DIRECTORY = "pet_voice";
    /** Id for loaders that register reload listeners by id. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-voice] ";

    public PetVoiceLoader() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        PetVoices.replaceAll(loaded.voices());
        Constants.LOG.info(LOG_PREFIX + "loaded {} pet voices", loaded.voices().size());
    }

    /**
     * @param files parsed JSON by file id
     * @return the voices that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<Identifier, JsonElement> files) {
        Map<Identifier, PetVoice> voices = new HashMap<>();
        List<String> errors = new ArrayList<>();
        files.forEach((entityId, json) -> PetVoice.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(voice -> voices.put(entityId, voice))
            .ifError(error -> errors.add(LOG_PREFIX + entityId + " says nothing, failed to parse: " + error.message())));
        return new Loaded(voices, errors);
    }

    /**
     * @param voices decoded voices by entity type id
     * @param errors one message per file that could not be decoded
     */
    record Loaded(Map<Identifier, PetVoice> voices, List<String> errors) {
    }
}
