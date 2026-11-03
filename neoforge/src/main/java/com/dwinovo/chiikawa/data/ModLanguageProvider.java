package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Chiikawa;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class ModLanguageProvider extends LanguageProvider {
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, Chiikawa.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        LanguageData.addTranslations(locale, this::add);
    }
}


