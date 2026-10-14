package com.dwinovo.chiikawa.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.RegistryPatchGenerator;

/**
 * Writes the recipes, the advancement and the loot tables, which are registry entries since 26.3,
 * the way the game writes its own: bootstrapped over the vanilla ones and dumped as a reloadable
 * layer. NeoForge has its own event for it.
 */
public final class FabricModReloadableRegistries {
    private FabricModReloadableRegistries() {
    }

    /**
     * @param world the registries of the world, which the recipes and the rest are made over
     * @param registries those and the reloadable ones the game has, which the mod's entries go beside
     */
    public static DataProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> world,
            CompletableFuture<HolderLookup.Provider> registries) {
        RegistrySetBuilder builder = new RegistrySetBuilder()
            .add(Registries.LOOT_TABLE, new ModLootTableProvider())
            .add(Registries.ADVANCEMENT, new AdvancementProvider(List.of(ModAdvancementProvider::new)))
            .add(ModRecipeProvider.bootstrap());
        return RegistriesDatapackGenerator.forReloadableLayer(output, RegistryPatchGenerator
            .createReloadableLookup(world, registries, builder).thenApply(RegistrySetBuilder.PatchedRegistries::patches));
    }
}
