package com.dwinovo.chiikawa.task;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for the labor board's levels,
 * {@code data/chiikawa/labor_board/levels.json}: one file for every board, which a data
 * pack replaces whole. Missing or broken, boards go by {@link BoardLevels#NONE} and put
 * nothing up, with a message saying why — as a shop whose price list is gone deals in
 * nothing.
 */
public final class BoardLevelsLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "labor_board";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);
    /** The file the levels are in. */
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "levels");

    private static final String LOG_PREFIX = "[chiikawa-board] ";

    public BoardLevelsLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        BoardLevels.replace(loaded.levels());
        Constants.LOG.info(LOG_PREFIX + "labor boards go up to level {}", loaded.levels().top());
    }

    /**
     * @param files parsed JSON by file id
     * @return the levels, or {@link BoardLevels#NONE} with the reason in the errors
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.keySet().stream()
            .filter(id -> !id.equals(FILE))
            .sorted()
            .forEach(id -> warnings.add(LOG_PREFIX + id + " is not read: every board's levels are in " + FILE));
        JsonElement json = files.get(FILE);
        if (json == null) {
            errors.add(LOG_PREFIX + "no " + FILE + " in any data pack, so labor boards put nothing up");
            return new Loaded(BoardLevels.NONE, errors, warnings);
        }
        DataResult<BoardLevels> parsed = BoardLevels.CODEC.parse(JsonOps.INSTANCE, json);
        parsed.error().ifPresent(error -> errors.add(
            LOG_PREFIX + FILE + " failed to parse, so labor boards put nothing up: " + error.message()));
        return new Loaded(parsed.result().orElse(BoardLevels.NONE), errors, warnings);
    }

    /**
     * @param levels what every board goes by
     * @param errors why the levels are {@link BoardLevels#NONE}, if they are
     * @param warnings one message per file in the directory that is not the levels
     */
    record Loaded(BoardLevels levels, List<String> errors, List<String> warnings) {
    }
}
