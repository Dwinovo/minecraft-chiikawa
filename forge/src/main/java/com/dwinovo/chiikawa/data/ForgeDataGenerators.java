package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.core.Cloner;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import com.mojang.serialization.Lifecycle;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistriesHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.CompletableFuture;
import java.util.Set;
import java.util.stream.Stream;

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

        // Datapack registries (Forge 1.20.3): use RegistrySetBuilder + RegistriesDatapackGenerator.
        RegistrySetBuilder patchBuilder = new RegistrySetBuilder()
            .add(ForgeRegistries.Keys.BIOME_MODIFIERS, ForgeModBiomeModifiers::bootstrap);

        CompletableFuture<HolderLookup.Provider> patchedRegistries = lookupProvider.thenApply(original -> {
            RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

            Cloner.Factory clonerFactory = new Cloner.Factory();
            DataPackRegistriesHooks.getDataPackRegistriesWithDimensions()
                .forEach(data -> data.runWithArguments(clonerFactory::addCodec));

            HolderLookup.Provider base = original;
            if (original.lookup(ForgeRegistries.Keys.BIOME_MODIFIERS).isEmpty()) {
                MappedRegistry<?> emptyBiomeModifiers = new MappedRegistry<>(
                    ForgeRegistries.Keys.BIOME_MODIFIERS,
                    Lifecycle.stable()
                );
                base = HolderLookup.Provider.create(Stream.concat(
                    original.listRegistries().map(original::lookupOrThrow),
                    Stream.of(emptyBiomeModifiers.asLookup())
                ));
            }

            return patchBuilder.buildPatch(registryAccess, base, clonerFactory).full();
        });

        generator.addProvider(event.includeServer(), new RegistriesDatapackGenerator(
            output,
            patchedRegistries,
            Set.of(Constants.MOD_ID)
        ));
    }
}
