package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/<namespace>/pet_errand/<errand>.json}. A file that
 * fails to parse, or that fails the checks the other files are needed for, is skipped with a message,
 * and its errand can no longer be started: an errand with broken settings should do nothing, not
 * take the world down with it. A file that is not named for an errand is only warned about.
 */
public final class ErrandSettingsLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_errand";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-errand] ";

    public ErrandSettingsLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        ErrandSettings.replaceAll(loaded.settings());
        Constants.LOG.info(LOG_PREFIX + "loaded settings for {} errands", loaded.settings().size());
    }

    /**
     * @param manager the data packs in force
     * @return the id of every errand file the packs have: the one way to ask what errands the packs have, since what
     *         {@link ErrandSettings#all} holds is only the files that were valid, and not yet when another loader runs
     */
    public static Set<ResourceLocation> errandIds(ResourceManager manager) {
        FileToIdConverter files = FileToIdConverter.json(DIRECTORY);
        return files.listMatchingResources(manager).keySet().stream().map(files::fileToId).collect(Collectors.toSet());
    }

    /**
     * @param files parsed JSON by file id, which is the id of the errand
     * @return the settings that decoded and hold together, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, ErrandSettings<?>> decoded = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((id, json) -> {
            if (Errands.get(id) == null) {
                warnings.add(LOG_PREFIX + id + " is skipped, there is no such errand");
                return;
            }
            ErrandSettings.codecOf(id).parse(JsonOps.INSTANCE, json)
                .ifSuccess(parsed -> decoded.put(id, parsed))
                .ifError(error -> errors.add(LOG_PREFIX + id + " is switched off, failed to parse: " + error.message()));
        });
        // What one file says about the others can only be checked with all of them read, and what only the
        // errand knows to ask of its own, with the errand.
        Map<ResourceLocation, ErrandSettings<?>> settings = new HashMap<>();
        decoded.forEach((id, parsed) -> parsed.problemAmong(id, decoded).or(() -> parsed.problemFor(Errands.get(id)))
            .ifPresentOrElse(
                problem -> errors.add(LOG_PREFIX + id + " is switched off, " + problem),
                () -> settings.put(id, parsed)));
        return new Loaded(settings, errors, warnings);
    }

    /**
     * @param settings decoded settings by errand id
     * @param errors one message per file that could not be used
     * @param warnings one message per file that is named for no errand
     */
    record Loaded(Map<ResourceLocation, ErrandSettings<?>> settings, List<String> errors, List<String> warnings) {
    }
}
