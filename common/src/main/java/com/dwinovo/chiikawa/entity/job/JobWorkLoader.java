package com.dwinovo.chiikawa.entity.job;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettingsLoader;
import com.dwinovo.chiikawa.init.InitRegistry;
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
 * Server data reload listener for {@code data/<namespace>/pet_job/<job>.json}. A file that fails to parse, or that is
 * named for no job, is skipped with a message, and its job offers the generic errands alone: a job with broken work
 * should do nothing, not take the world down with it. Errand ids that no pack has a file for are only warned about.
 */
public final class JobWorkLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_job";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-job] ";

    public JobWorkLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files, InitRegistry.PET_JOB_REGISTRY.keySet(), ErrandSettingsLoader.errandIds(manager));
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        JobWork.replaceAll(loaded.work());
        Constants.LOG.info(LOG_PREFIX + "loaded the work of {} jobs", loaded.work().size());
    }

    /**
     * @param files parsed JSON by file id, which is the id of the job
     * @param jobs the ids of the jobs there are
     * @param errands the ids of the errands the packs have files for, see {@link ErrandSettingsLoader#errandIds}
     * @return the work that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files, Set<ResourceLocation> jobs, Set<ResourceLocation> errands) {
        Map<ResourceLocation, JobWork> work = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((id, json) -> {
            if (!jobs.contains(id)) {
                warnings.add(LOG_PREFIX + id + " is skipped, there is no such job");
                return;
            }
            JobWork.CODEC.parse(JsonOps.INSTANCE, json)
                .ifSuccess(parsed -> {
                    work.put(id, parsed);
                    parsed.work().stream()
                        .filter(errand -> !errands.contains(errand))
                        .forEach(errand -> warnings.add(LOG_PREFIX + id + " names unknown errand " + errand));
                })
                .ifError(error -> errors.add(LOG_PREFIX + id + " offers no work, failed to parse: " + error.message()));
        });
        return new Loaded(work, errors, warnings);
    }

    /**
     * @param work decoded work by job id
     * @param errors one message per file that could not be decoded
     * @param warnings one message per file named for no job, and per errand id that is no errand's
     */
    record Loaded(Map<ResourceLocation, JobWork> work, List<String> errors, List<String> warnings) {
    }
}
