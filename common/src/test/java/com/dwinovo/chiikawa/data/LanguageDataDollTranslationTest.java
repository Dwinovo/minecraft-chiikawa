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
        "item.chiikawa.rakko_doll"
    };

    @Test
    void englishLocaleIncludesAllDollTranslations() {
        Map<String, String> translations = collect("en_us");
        for (String key : DOLL_KEYS) {
            assertTrue(translations.containsKey(key), () -> "missing translation key: " + key);
        }
    }

    @Test
    void chineseLocaleIncludesAllDollTranslations() {
        Map<String, String> translations = collect("zh_cn");
        for (String key : DOLL_KEYS) {
            assertTrue(translations.containsKey(key), () -> "missing translation key: " + key);
        }
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
