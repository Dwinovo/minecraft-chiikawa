package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;

import com.dwinovo.chiikawa.Chiikawa;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(modid = Chiikawa.MODID)
public final class DataGenerators {
    private DataGenerators() {
    }

    /** The biome modifier, the recipes, the advancement and the loot tables: all registry entries, written with the rest of them. */
    @SubscribeEvent
    public static void gatherRegistryEntries(GatherDataRegistryEntriesEvent event) {
        event.gatherFor(Chiikawa.MODID)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap)
            .add(ModRecipeProvider.bootstrap())
            .advancement(ModAdvancementProvider::new)
            .add(Registries.LOOT_TABLE, new ModLootTableProvider());
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        event.getGenerator().addProvider(true,
                new ModSoundDefinitionsProvider(output));
        event.getGenerator().addProvider(true,
                new ModItemModelProvider(output));
        event.getGenerator().addProvider(true, new PropAtlasProvider(output));
        event.getGenerator().addProvider(true, new ManualProvider(output));
        for (String locale : LanguageData.LOCALES) {
            event.getGenerator().addProvider(true, new ModLanguageProvider(output, locale));
        }
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getWorldLookupProvider();

        ModBlockTagsProvider blockTagsProvider = new ModBlockTagsProvider(output, lookupProvider);
        event.getGenerator().addProvider(true, blockTagsProvider);
        event.getGenerator().addProvider(true,
                new ModItemTagsProvider(output, lookupProvider));
        event.getGenerator().addProvider(true, new PetPersonalityProvider(output));
        event.getGenerator().addProvider(true, new PetVoiceProvider(output));
        event.getGenerator().addProvider(true, new PetTaskTypeProvider(output));
        event.getGenerator().addProvider(true, new GameTestStructureProvider(output));
        event.getGenerator().addProvider(true, new ShopCatalogProvider(output));
        event.getGenerator().addProvider(true, new LaborBoardLevelProvider(output));
        event.getGenerator().addProvider(true, new WhistleSettingsProvider(output));
        event.getGenerator().addProvider(true, new CameraSettingsProvider(output));
        event.getGenerator().addProvider(true, new GiftingSettingsProvider(output));
        event.getGenerator().addProvider(true, new AnchorSettingsProvider(output));
        event.getGenerator().addProvider(true, new PetSpawnProvider(output));
        event.getGenerator().addProvider(true, new PetInteractionProvider(output));
        event.getGenerator().addProvider(true, new QualificationProvider(output));
        // Entity tags.
        event.getGenerator().addProvider(true, 
            new ModEntityTagsProvider(
                output,
                lookupProvider
            )
        );
    }
}
