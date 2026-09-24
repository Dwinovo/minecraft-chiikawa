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
        "tag.item.chiikawa.entity_pickable_items",
        "tag.item.chiikawa.pet_treats"
    };

    @Test
    void everyLocaleIncludesItemTagTranslations() {
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = collect(locale);
            for (String key : ITEM_TAG_KEYS) {
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
