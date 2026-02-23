package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LanguageDataTagTranslationTest {
    private static final String[] ITEM_TAG_KEYS = new String[] {
        "tag.item.chiikawa.entity_farmer_tools",
        "tag.item.chiikawa.entity_fencer_tools",
        "tag.item.chiikawa.entity_archer_tools",
        "tag.item.chiikawa.entity_tame_foods",
        "tag.item.chiikawa.entity_plant_crops",
        "tag.item.chiikawa.entity_deliver_items",
        "tag.item.chiikawa.entity_pickable_items"
    };

    @Test
    void englishLocaleIncludesItemTagTranslations() {
        Map<String, String> translations = collect("en_us");
        for (String key : ITEM_TAG_KEYS) {
            assertTrue(translations.containsKey(key), () -> "missing translation key: " + key);
        }
    }

    @Test
    void chineseLocaleIncludesItemTagTranslations() {
        Map<String, String> translations = collect("zh_cn");
        for (String key : ITEM_TAG_KEYS) {
            assertTrue(translations.containsKey(key), () -> "missing translation key: " + key);
        }
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
