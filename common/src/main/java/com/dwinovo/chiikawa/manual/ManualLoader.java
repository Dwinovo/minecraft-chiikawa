package com.dwinovo.chiikawa.manual;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Client resource reload listener for {@code assets/<namespace>/manual/<id>.json}: the
 * handbook's pages. A page that fails to parse is left out with a message, and the rest of
 * the book is still there to read.
 */
public final class ManualLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "manual";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-manual] ";

    public ManualLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files);
        loaded.errors().forEach(Constants.LOG::error);
        ManualPages.replaceAll(loaded.pages());
        Constants.LOG.info(LOG_PREFIX + "loaded {} handbook pages", loaded.pages().size());
    }

    /**
     * @param files parsed JSON by file id
     * @return the pages that decoded, in reading order (by {@code order}, then by id), and
     *         what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files) {
        List<Map.Entry<ResourceLocation, ManualPage>> pages = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        files.forEach((id, json) -> ManualPage.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(page -> pages.add(Map.entry(id, page)))
            .ifError(error -> errors.add(LOG_PREFIX + id + " is left out, failed to parse: " + error.message())));
        pages.sort(Comparator.<Map.Entry<ResourceLocation, ManualPage>>comparingInt(entry -> entry.getValue().order())
            .thenComparing(Map.Entry::getKey));
        return new Loaded(pages.stream().map(Map.Entry::getValue).toList(), errors);
    }

    /**
     * @param pages the pages in reading order
     * @param errors one message per file that could not be decoded
     */
    record Loaded(List<ManualPage> pages, List<String> errors) {
    }
}
