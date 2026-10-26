package com.dwinovo.chiikawa.entity.brain.errand.remark;

import com.dwinovo.chiikawa.Constants;
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
 * Server data reload listener for {@code data/<namespace>/pet_remark/<id>.json}. A file that fails to parse is skipped,
 * so that remark is not made until it is fixed.
 */
public final class PetRemarkLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_remark";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-remark] ";

    public PetRemarkLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        PetRemarks.replaceAll(loaded.remarks());
        Constants.LOG.info(LOG_PREFIX + "loaded {} pet remarks", loaded.remarks().size());
    }

    /**
     * @param files parsed JSON by file id
     * @return the remarks that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, PetRemark> remarks = new HashMap<>();
        List<String> errors = new ArrayList<>();
        files.forEach((id, json) -> PetRemark.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(remark -> remarks.put(id, remark))
            .ifError(error -> errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message())));
        return new Loaded(remarks, errors);
    }

    /**
     * @param remarks decoded remarks by id
     * @param errors one message per file that could not be decoded
     */
    record Loaded(Map<ResourceLocation, PetRemark> remarks, List<String> errors) {
    }
}
