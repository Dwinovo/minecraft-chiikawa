package com.dwinovo.chiikawa.manual;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Client resource reload listener for {@code assets/<namespace>/manual/<id>.json}: the
 * handbook's pages. A page that fails to parse is left out with a message, and the rest of
 * the book is still there to read.
 */
public final class ManualLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final String DIRECTORY = "manual";
    /** Id for loaders that register reload listeners by id. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-manual] ";

    public ManualLoader() {
        // Read as plain JSON, so each file is decoded here and a bad one is reported by name.
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
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
    static Loaded load(Map<Identifier, JsonElement> files) {
        List<Map.Entry<Identifier, ManualPage>> pages = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        files.forEach((id, json) -> ManualPage.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(page -> pages.add(Map.entry(id, page)))
            .ifError(error -> errors.add(LOG_PREFIX + id + " is left out, failed to parse: " + error.message())));
        pages.sort(Comparator.<Map.Entry<Identifier, ManualPage>>comparingInt(entry -> entry.getValue().order())
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
