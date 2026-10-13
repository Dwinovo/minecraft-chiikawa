package com.dwinovo.chiikawa.entity.brain.constraint;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/chiikawa/pet_anchor/anchor.json}. A file that
 * fails to parse is reported and ignored, and with it every pet stays where it is: a pack that
 * has broken the settings should not have pets guessing how far they may go.
 */
public final class AnchorSettingsLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_anchor";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, DIRECTORY);
    /** The one file the settings are in. */
    public static final ResourceLocation FILE = new ResourceLocation(Constants.MOD_ID, "anchor");

    private static final String LOG_PREFIX = "[chiikawa-anchor] ";

    public AnchorSettingsLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Optional<AnchorSettings> settings = load(files);
        AnchorSettings.replace(settings);
        if (settings.isPresent()) {
            Constants.LOG.info(LOG_PREFIX + "loaded the anchor settings");
        } else {
            Constants.LOG.error(LOG_PREFIX + "no anchor settings: pets stay where they are");
        }
    }

    /**
     * @param files parsed JSON by file id
     * @return the settings, or nothing when there is no such file or it does not decode
     */
    static Optional<AnchorSettings> load(Map<ResourceLocation, JsonElement> files) {
        JsonElement json = files.get(FILE);
        if (json == null) {
            return Optional.empty();
        }
        return AnchorSettings.CODEC.parse(JsonOps.INSTANCE, json)
            .ifError(error -> Constants.LOG.error(LOG_PREFIX + FILE + " is skipped, failed to parse: " + error.message()))
            .result();
    }
}
