package com.dwinovo.chiikawa.data;

import java.util.concurrent.CompletableFuture;

import com.dwinovo.chiikawa.Chiikawa;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.Set;

@EventBusSubscriber(modid = Chiikawa.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        boolean client = event.includeClient();
        boolean server = event.includeServer();
        event.getGenerator().addProvider(client,
                new ModSoundDefinitionsProvider(output, existingFileHelper));
        event.getGenerator().addProvider(client,
                new ModItemModelProvider(output, existingFileHelper));
        event.getGenerator().addProvider(client,
                new ModLanguageProvider(output, "en_us"));
        event.getGenerator().addProvider(client,
                new ModLanguageProvider(output, "zh_cn"));
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.getGenerator().addProvider(server,
                new DatapackBuiltinEntriesProvider(output, lookupProvider, ModDatapackRegistries.BUILDER,
                        Set.of(Chiikawa.MODID)));
        ModBlockTagsProvider blockTagsProvider = new ModBlockTagsProvider(output, lookupProvider, existingFileHelper);
        event.getGenerator().addProvider(server, blockTagsProvider);
        event.getGenerator().addProvider(server,
                new ModItemTagsProvider(output, lookupProvider, existingFileHelper));
        event.getGenerator().addProvider(server, new ModRecipeProvider(output, lookupProvider));
        // Entity tags.
        event.getGenerator().addProvider(server,
            new ModEntityTagsProvider(
                output,
                lookupProvider,
                existingFileHelper
            )
        );
    }
}
