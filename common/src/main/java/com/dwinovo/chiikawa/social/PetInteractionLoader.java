package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.Constants;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Server data reload listener for {@code data/<namespace>/pet_interaction/<id>.json}. A
 * file that fails to parse is skipped; a scene naming a pet that does not exist is kept
 * but warned about, since that part can never be played.
 */
public final class PetInteractionLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final String DIRECTORY = "pet_interaction";
    /** Id for loaders that register reload listeners by id. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    private static final String LOG_PREFIX = "[chiikawa-social] ";

    public PetInteractionLoader() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Loaded loaded = load(files, BuiltInRegistries.ENTITY_TYPE::containsKey);
        loaded.errors().forEach(Constants.LOG::error);
        loaded.warnings().forEach(Constants.LOG::warn);
        PetInteractions.replaceAll(loaded.interactions());
        Constants.LOG.info(LOG_PREFIX + "loaded {} pet interactions", loaded.interactions().size());
    }

    /**
     * @param files parsed JSON by file id
     * @param knownEntity whether an entity type id is registered
     * @return the scenes that decoded, and what went wrong with the rest
     */
    static Loaded load(Map<ResourceLocation, JsonElement> files, Predicate<ResourceLocation> knownEntity) {
        Map<ResourceLocation, PetInteraction> interactions = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        files.forEach((id, json) -> PetInteraction.CODEC.parse(JsonOps.INSTANCE, json)
            .ifSuccess(interaction -> {
                interactions.put(id, interaction);
                Stream.concat(interaction.initiators().stream(), interaction.partners().stream())
                    .flatMap(side -> side.pets().stream())
                    .filter(pet -> !pet.tag())
                    .map(ExtraCodecs.TagOrElementLocation::id)
                    .filter(pet -> !knownEntity.test(pet))
                    .distinct()
                    .forEach(pet -> warnings.add(LOG_PREFIX + id + " names unknown pet " + pet));
            })
            .ifError(error -> errors.add(LOG_PREFIX + id + " is skipped, failed to parse: " + error.message())));
        return new Loaded(interactions, errors, warnings);
    }

    /**
     * @param interactions decoded scenes by id
     * @param errors one message per file that could not be decoded
     * @param warnings one message per unknown pet
     */
    record Loaded(Map<ResourceLocation, PetInteraction> interactions, List<String> errors, List<String> warnings) {
    }
}
