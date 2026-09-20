package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Everything the player reads about a pet's work has a name in both languages. */
class LanguageDataUiTranslationTest {
    private static final String[] LOCALES = {"en_us", "zh_cn"};
    private static final String[] SCREEN_KEYS = {
        "screen.chiikawa.labor_board",
        "screen.chiikawa.labor_board.empty",
        "screen.chiikawa.labor_board.slip",
        "screen.chiikawa.labor_board.open",
        "screen.chiikawa.labor_board.taken",
        "screen.chiikawa.pet.doing",
        "screen.chiikawa.pet.doing.nothing",
        "screen.chiikawa.pet.slip",
        "screen.chiikawa.pet.slip.none",
        "block.chiikawa.labor_board"
    };

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void everyIntentHasAName() {
        for (String locale : LOCALES) {
            Map<String, String> translations = collect(locale);
            for (PetIntent intent : PetIntents.all()) {
                String key = "intent." + intent.id().getNamespace() + "." + intent.id().getPath();
                assertTrue(translations.containsKey(key), () -> locale + " is missing " + key);
            }
        }
    }

    @Test
    void everySlipTypeHasAName() {
        for (String locale : LOCALES) {
            Map<String, String> translations = collect(locale);
            for (ResourceLocation type : List.of(PetTaskTypeData.WEEDING, PetTaskTypeData.MUSHROOM_PICKING,
                    PetTaskTypeData.STREET_PERFORMANCE)) {
                String key = "pet_task." + type.getNamespace() + "." + type.getPath();
                assertTrue(translations.containsKey(key), () -> locale + " is missing " + key);
            }
        }
    }

    @Test
    void everyScreenLineHasATranslation() {
        for (String locale : LOCALES) {
            Map<String, String> translations = collect(locale);
            for (String key : SCREEN_KEYS) {
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
