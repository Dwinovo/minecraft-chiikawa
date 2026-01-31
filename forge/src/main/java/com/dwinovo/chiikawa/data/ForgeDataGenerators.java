package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeDataGenerators {
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Language providers
        generator.addProvider(event.includeClient(), new ForgeModLanguageProvider(output, "en_us"));
        generator.addProvider(event.includeClient(), new ForgeModLanguageProvider(output, "zh_cn"));
        
        // Item model provider
        generator.addProvider(event.includeClient(), new ForgeModItemModelProvider(output, existingFileHelper));
        
        // Tag providers
        ForgeModBlockTagsProvider blockTagsProvider = new ForgeModBlockTagsProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), new ForgeModItemTagsProvider(output, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), new ForgeModEntityTagsProvider(output, lookupProvider, existingFileHelper));
        
        // Recipe provider
        generator.addProvider(event.includeServer(), new ForgeModRecipeProvider(output));
        
        // Sound provider
        generator.addProvider(event.includeClient(), new ForgeModSoundsProvider(output));

        // Datapack provider for Biome Modifiers
        generator.addProvider(event.includeServer(), new net.minecraftforge.common.data.DatapackBuiltinEntriesProvider(
            output,
            lookupProvider,
            new net.minecraft.core.RegistrySetBuilder()
                .add(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, ForgeModBiomeModifiers::bootstrap),
            java.util.Set.of(Constants.MOD_ID)
        ));
    }
}
