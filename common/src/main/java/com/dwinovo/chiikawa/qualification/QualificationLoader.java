package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.Constants;
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
 * Server data reload listener for {@code data/<namespace>/pet_qualification/<id>.json}. A
 * file that fails to parse is skipped; a licence whose practice is a slip type no data
 * pack has is kept but warned about, since no pet could ever practise for it.
 */
public final class QualificationLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final String DIRECTORY = "pet_qualification";
    /** Id for loaders that register reload listeners by id. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-licence] ";

    private final Predicate<Identifier> knownTask;

    /** @param knownTask whether a slip type is loaded, asked once the slip types have been */
    public QualificationLoader(Predicate<Identifier> knownTask) {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
        this.knownTask = knownTask;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files, knownTask);
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        Qualifications.replaceAll(loaded.qualifications());
        Constants.LOG.info(LOG_PREFIX + "loaded {} pet licences", loaded.qualifications().size());
    }

    /**
     * @param files parsed JSON by file id
     * @param knownTask whether a slip type is loaded
     * @return the licences that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<Identifier, JsonElement> files, Predicate<Identifier> knownTask) {
        Map<Identifier, Qualification> qualifications = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((id, json) -> Qualification.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(qualification -> {
                qualifications.put(id, qualification);
                if (!knownTask.test(qualification.practiceTask())) {
                    warnings.add(LOG_PREFIX + id + " is practised by unknown slip type " + qualification.practiceTask());
                }
            })
            .ifError(error -> errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message())));
        return new Loaded(qualifications, errors, warnings);
    }

    /**
     * @param qualifications decoded licences by id
     * @param errors one message per file that could not be decoded
     * @param warnings one message per licence practised by a slip type no pack has
     */
    record Loaded(Map<Identifier, Qualification> qualifications, List<String> errors, List<String> warnings) {
    }
}
