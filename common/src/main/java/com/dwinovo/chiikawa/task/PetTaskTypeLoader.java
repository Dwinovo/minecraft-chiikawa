package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/<namespace>/pet_task/<id>.json}. A file
 * that fails to parse is skipped; a type naming a capability or work counter that does
 * not exist is kept but warned about, since no pet could ever take or finish it.
 */
public final class PetTaskTypeLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final String DIRECTORY = "pet_task";
    /** Id for loaders that register reload listeners by id. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-task] ";

    public PetTaskTypeLoader() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files, InitRegistry.PET_JOB_REGISTRY::containsKey);
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        PetTaskTypes.replaceAll(loaded.types());
        Constants.LOG.info(LOG_PREFIX + "loaded {} pet task types", loaded.types().size());
    }

    /**
     * @param files parsed JSON by file id
     * @param knownCapability whether a capability id is registered
     * @return the types that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<Identifier, JsonElement> files, Predicate<Identifier> knownCapability) {
        Map<Identifier, PetTaskType> types = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((id, json) -> PetTaskType.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(type -> {
                types.put(id, type);
                if (!knownCapability.test(type.capability())) {
                    warnings.add(LOG_PREFIX + id + " is for unknown capability " + type.capability());
                }
                if (!PetWorkCounters.ALL.contains(type.counter())) {
                    warnings.add(LOG_PREFIX + id + " counts unknown work " + type.counter());
                }
            })
            .ifError(error -> errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message())));
        return new Loaded(types, errors, warnings);
    }

    /**
     * @param types decoded task types by id
     * @param errors one message per file that could not be decoded
     * @param warnings one message per unknown capability or work counter
     */
    record Loaded(Map<Identifier, PetTaskType> types, List<String> errors, List<String> warnings) {
    }
}
