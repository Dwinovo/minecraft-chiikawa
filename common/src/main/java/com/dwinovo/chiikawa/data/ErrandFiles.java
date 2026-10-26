package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkFile;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@code pet_errand} files as the data generator writes them: the settings of every errand, and beside the
 * settings of a work its definition, see {@link WorkData}. The one place they are put together, so that what is
 * written and what a test loads cannot differ.
 */
public final class ErrandFiles {
    private ErrandFiles() {
    }

    /** @return the JSON of each errand's file, by the id of the errand */
    public static Map<ResourceLocation, JsonElement> all() {
        Map<ResourceLocation, Work<?, ?>> works = WorkData.all();
        Map<ResourceLocation, JsonElement> files = new TreeMap<>();
        ErrandSettingsData.all().forEach((id, settings) -> {
            Work<?, ?> work = works.get(id);
            files.put(id, work == null
                ? ErrandSettings.codecOf(id).encodeStart(JsonOps.INSTANCE, settings).getOrThrow()
                : WorkFile.CODEC.encodeStart(JsonOps.INSTANCE, fileOf(work, settings)).getOrThrow());
        });
        works.keySet().forEach(id -> {
            if (!files.containsKey(id)) {
                throw new IllegalStateException(id + " is a work and has no settings");
            }
        });
        return files;
    }

    /** @param settings the settings of the work, which the work's action gave the tuning its shape */
    @SuppressWarnings("unchecked")
    private static <P extends Plan, T> WorkFile<P, T> fileOf(Work<P, T> work, ErrandSettings<?> settings) {
        return new WorkFile<>(work, (ErrandSettings<T>) settings);
    }
}
