package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;

import com.dwinovo.chiikawa.Chiikawa;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.Set;

@EventBusSubscriber(modid = Chiikawa.MODID)
public final class DataGenerators {
    private DataGenerators() {
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
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.getGenerator().addProvider(true,
                new DatapackBuiltinEntriesProvider(output, lookupProvider, ModDatapackRegistries.BUILDER,
                        Set.of(Chiikawa.MODID)));
        ModBlockTagsProvider blockTagsProvider = new ModBlockTagsProvider(output, lookupProvider, existingFileHelper);
        event.getGenerator().addProvider(server, blockTagsProvider);
        event.getGenerator().addProvider(server,
                new ModItemTagsProvider(output, lookupProvider, existingFileHelper));
        event.getGenerator().addProvider(server, new ModRecipeProvider(output, lookupProvider));
        event.getGenerator().addProvider(server, new PetPersonalityProvider(output));
        event.getGenerator().addProvider(server, new PetVoiceProvider(output));
        event.getGenerator().addProvider(server, new PetTaskTypeProvider(output));
        event.getGenerator().addProvider(server, new GameTestStructureProvider(output));
        event.getGenerator().addProvider(server, new ShopCatalogProvider(output));
        event.getGenerator().addProvider(server, new LaborBoardLevelProvider(output));
        event.getGenerator().addProvider(server, new WhistleSettingsProvider(output));
        event.getGenerator().addProvider(server, new CameraSettingsProvider(output));
        event.getGenerator().addProvider(server, new PetSpawnProvider(output));
        event.getGenerator().addProvider(server, new PetInteractionProvider(output));
        event.getGenerator().addProvider(server, new QualificationProvider(output));
        event.getGenerator().addProvider(server, new ModAdvancementProvider(output, lookupProvider));
        event.getGenerator().addProvider(server, new ModLootTableProvider(output, lookupProvider));
        // Entity tags.
        event.getGenerator().addProvider(true, 
            new ModEntityTagsProvider(
                output,
                lookupProvider
            )
        );
    }
}
