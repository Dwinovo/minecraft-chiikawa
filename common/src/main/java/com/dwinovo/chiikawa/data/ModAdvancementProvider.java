package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;

/**
 * The mod's advancements. Only one so far, and shown nowhere: a player is handed the
 * handbook on their first tick in a world, once — the way data packs have always given a
 * newcomer something. A pack that would rather not replaces it. Shared by both loaders'
 * data generators.
 */
public final class ModAdvancementProvider extends AdvancementSubProvider {
    public static final Identifier HANDBOOK = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "handbook");

    public ModAdvancementProvider(BootstrapContext<Advancement> output) {
        super(output);
    }

    @Override
    public void generate() {
        // Vanilla's plain builder, named for recipes: the other one reports every
        // advancement earned to Mojang's telemetry, which a hidden gift has no business in.
        Advancement.Builder.recipeAdvancement()
            .addCriterion("arrived", PlayerTrigger.TriggerInstance.tick())
            .rewards(AdvancementRewards.Builder.loot(output.lookup(Registries.LOOT_TABLE).getOrThrow(ModLootTableProvider.HANDBOOK_GIFT)))
            .build(HANDBOOK)
            .register(output);
    }
}
