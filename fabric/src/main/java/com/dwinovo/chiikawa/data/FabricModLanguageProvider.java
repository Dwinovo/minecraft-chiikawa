package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.data.LanguageData;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public class FabricModLanguageProvider extends FabricLanguageProvider {
    private final String locale;

    public FabricModLanguageProvider(FabricPackOutput output, String locale, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, locale, registriesFuture);
        this.locale = locale;
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
        LanguageData.addTranslations(locale, builder::add);
    }
}
