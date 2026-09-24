package com.dwinovo.chiikawa.spawn;

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
 * Server data reload listener for {@code data/<namespace>/pet_spawn/<id>.json}. A file that
 * fails to parse is skipped with a message: the pets in it stop turning up, and the rest
 * carry on.
 */
public final class PetSpawnLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_spawn";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-spawn] ";

    public PetSpawnLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        PetSpawns.replaceAll(loaded.spawns());
        Constants.LOG.info(LOG_PREFIX + "loaded {} wild pet spawn lists", loaded.spawns().size());
    }

    /**
     * @param files parsed JSON by file id
     * @return the spawn lists that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, PetSpawn> spawns = new HashMap<>();
        List<String> errors = new ArrayList<>();
        files.forEach((id, json) -> {
            DataResult<PetSpawn> parsed = PetSpawn.CODEC.parse(JsonOps.INSTANCE, json);
            parsed.result().ifPresent(spawn -> spawns.put(id, spawn));
            parsed.error().ifPresent(error -> errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message()));
        });
        return new Loaded(spawns, errors);
    }

    /**
     * @param spawns decoded spawn lists by id
     * @param errors one message per file that could not be decoded
     */
    record Loaded(Map<ResourceLocation, PetSpawn> spawns, List<String> errors) {
    }
}
