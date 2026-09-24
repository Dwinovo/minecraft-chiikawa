package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LanguageDataDollTranslationTest {
    private static final String[] DOLL_KEYS = new String[] {
        "item.chiikawa.usagi_doll",
        "item.chiikawa.hachiware_doll",
        "item.chiikawa.chiikawa_doll",
        "item.chiikawa.shisa_doll",
        "item.chiikawa.momonga_doll",
        "item.chiikawa.kurimanju_doll",
        "item.chiikawa.rakko_doll",
        "item.chiikawa.furuhonya_doll"
    };

    @Test
    void everyLocaleIncludesAllDollTranslations() {
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = collect(locale);
            for (String key : DOLL_KEYS) {
                assertTrue(translations.containsKey(key), () -> locale + " is missing " + key);
            }
        }
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
