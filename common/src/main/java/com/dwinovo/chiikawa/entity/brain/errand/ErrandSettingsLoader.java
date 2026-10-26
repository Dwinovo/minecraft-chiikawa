package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.Gson;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkFile;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
 * Server data reload listener for {@code data/<namespace>/pet_errand/<errand>.json}. A file with an
 * {@code "action"} is a work, see {@link WorkFile}, and its errand exists only while the file does; any
 * other file is the settings of the code errand it is named for. A file that fails to parse, or that fails
 * the checks the other files are needed for, is skipped with a message, and its errand can no longer be
 * started: an errand with broken settings should do nothing, not take the world down with it. A file that is
 * not named for an errand is only warned about.
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
        loaded.install();
        Constants.LOG.info(LOG_PREFIX + "loaded settings for {} errands, {} of them work", loaded.settings().size(),
            loaded.works().size());
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
     * @param manager the data packs in force
     * @return the id of every errand whose file says it is done to blocks the work sensor finds, so that the jobs that
     *         list them can be checked without another loader's state, as {@link #errandIds} does for what exists
     */
    public static Set<ResourceLocation> blockWorkIds(ResourceManager manager) {
        Map<ResourceLocation, JsonElement> files = new HashMap<>();
        scanDirectory(manager, DIRECTORY, new Gson(), files);
        return files.entrySet().stream().filter(file -> targetsBlocks(file.getValue())).map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /** @return whether a file names a target that is blocks the work sensor finds; a file that does not parse names none */
    private static boolean targetsBlocks(JsonElement json) {
        return json instanceof JsonObject file && file.has("target") && WorkTarget.CODEC.parse(JsonOps.INSTANCE, file.get("target"))
            .result().filter(WorkTarget.BlockTarget.class::isInstance).isPresent();
    }

    /**
     * @param files parsed JSON by file id, which is the id of the errand
     * @return the errands that decoded and hold together, and what went wrong with the rest
     */
    public static Loaded load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, ErrandSettings<?>> decoded = new HashMap<>();
        Map<ResourceLocation, ErrandType<?, ?>> types = new HashMap<>();
        Map<ResourceLocation, WorkErrand<?, ?>> works = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((id, json) -> {
            if (isWork(json)) {
                if (Errands.isCode(id)) {
                    errors.add(LOG_PREFIX + id + " is switched off, it is an errand written in code and cannot be a work");
                    return;
                }
                WorkFile.CODEC.parse(JsonOps.INSTANCE, json)
                    .ifSuccess(parsed -> {
                        WorkErrand<?, ?> errand = new WorkErrand<>(id, parsed.work());
                        works.put(id, errand);
                        types.put(id, errand);
                        decoded.put(id, parsed.settings());
                    })
                    .ifError(error -> errors.add(LOG_PREFIX + id + " is switched off, failed to parse: " + error.message()));
                return;
            }
            if (!Errands.isCode(id)) {
                warnings.add(LOG_PREFIX + id + " is skipped, there is no such errand");
                return;
            }
            ErrandSettings.codecOf(id).parse(JsonOps.INSTANCE, json)
                .ifSuccess(parsed -> {
                    types.put(id, Errands.get(id));
                    decoded.put(id, parsed);
                })
                .ifError(error -> errors.add(LOG_PREFIX + id + " is switched off, failed to parse: " + error.message()));
        });
        // What one file says about the others can only be checked with all of them read, and what only the
        // errand knows to ask of its own, with the errand.
        Map<ResourceLocation, ErrandSettings<?>> settings = new HashMap<>();
        decoded.forEach((id, parsed) -> parsed.problemAmong(id, decoded).or(() -> parsed.problemFor(types.get(id)))
            .ifPresentOrElse(
                problem -> {
                    errors.add(LOG_PREFIX + id + " is switched off, " + problem);
                    works.remove(id);
                },
                () -> settings.put(id, parsed)));
        return new Loaded(works, settings, errors, warnings);
    }

    private static boolean isWork(JsonElement json) {
        return json instanceof JsonObject object && object.has("action");
    }

    /**
     * @param works the work errands the files made, by id
     * @param settings the settings of every errand that decoded, work or code, by id
     * @param errors one message per file that could not be used
     * @param warnings one message per file that is named for no errand
     */
    public record Loaded(Map<ResourceLocation, WorkErrand<?, ?>> works, Map<ResourceLocation, ErrandSettings<?>> settings,
                         List<String> errors, List<String> warnings) {
        /** Makes these the errands and settings in force, see {@link Errands#install}. */
        public void install() {
            Errands.install(works, settings);
        }
    }
}
