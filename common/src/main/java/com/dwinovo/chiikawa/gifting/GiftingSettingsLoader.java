package com.dwinovo.chiikawa.gifting;

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
 * Server data reload listener for {@code data/chiikawa/pet_gifting/gifting.json}. A file that
 * fails to parse is reported and ignored, and with it no pet gives presents: a pack that has
 * broken the settings should not have pets guessing.
 */
public final class GiftingSettingsLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "pet_gifting";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);
    /** The one file the settings are in. */
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "gifting");

    private static final String LOG_PREFIX = "[chiikawa-gifting] ";

    public GiftingSettingsLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Optional<GiftingSettings> settings = load(files);
        GiftingSettings.replace(settings);
        Constants.LOG.info(LOG_PREFIX + (settings.isPresent() ? "loaded the gifting settings" : "no gifting settings: pets give no presents"));
    }

    /**
     * @param files parsed JSON by file id
     * @return the settings, or nothing when there is no such file or it does not decode
     */
    static Optional<GiftingSettings> load(Map<ResourceLocation, JsonElement> files) {
        JsonElement json = files.get(FILE);
        if (json == null) {
            return Optional.empty();
        }
        return GiftingSettings.CODEC.parse(JsonOps.INSTANCE, json)
            .ifError(error -> Constants.LOG.error(LOG_PREFIX + FILE + " is skipped, failed to parse: " + error.message()))
            .result();
    }
}
