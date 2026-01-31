package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ForgeModLanguageProvider extends LanguageProvider {
    private final String locale;

    public ForgeModLanguageProvider(PackOutput output, String locale) {
        super(output, Constants.MOD_ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        LanguageData.addTranslations(locale, this::add);
    }
}
