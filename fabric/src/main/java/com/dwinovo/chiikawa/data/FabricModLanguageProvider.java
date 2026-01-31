package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.data.LanguageData;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class FabricModLanguageProvider extends FabricLanguageProvider {
    private final String locale;

    public FabricModLanguageProvider(FabricDataOutput output, String locale) {
        super(output, locale);
        this.locale = locale;
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        LanguageData.addTranslations(locale, builder::add);
    }
}
