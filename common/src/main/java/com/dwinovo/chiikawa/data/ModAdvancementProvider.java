package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's advancements. Only one so far, and shown nowhere: a player is handed the
 * handbook on their first tick in a world, once — the way data packs have always given a
 * newcomer something. A pack that would rather not replaces it. Shared by both loaders'
 * data generators.
 */
public final class ModAdvancementProvider implements DataProvider {
    public static final ResourceLocation HANDBOOK = new ResourceLocation(Constants.MOD_ID, "handbook");

    private final PackOutput.PathProvider pathProvider;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(lookup -> {
            // Vanilla's plain builder, named for recipes: the other one reports every
            // advancement earned to Mojang's telemetry, which a hidden gift has no business in.
            Advancement handbook = Advancement.Builder.recipeAdvancement()
                .addCriterion("arrived", PlayerTrigger.TriggerInstance.tick())
                .rewards(AdvancementRewards.Builder.loot(ModLootTableProvider.HANDBOOK_GIFT))
                .build(HANDBOOK)
                .value();
            return DataProvider.saveStable(cache, lookup, Advancement.CODEC, handbook, pathProvider.json(HANDBOOK));
        });
    }

    @Override
    public String getName() {
        return "Chiikawa Advancements";
    }
}
