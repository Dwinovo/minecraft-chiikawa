package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Everything the player reads about a pet's work has a name in every language. */
class LanguageDataUiTranslationTest {
    private static final String[] SCREEN_KEYS = {
        "screen.chiikawa.labor_board",
        "screen.chiikawa.labor_board.count",
        "screen.chiikawa.labor_board.empty",
        "screen.chiikawa.labor_board.for_job",
        "screen.chiikawa.labor_board.open",
        "screen.chiikawa.labor_board.taken",
        "screen.chiikawa.labor_board.taken_by",
        "screen.chiikawa.pet.doing.nothing",
        "screen.chiikawa.pet.bag_hint",
        "screen.chiikawa.pet.tab.backpack",
        "screen.chiikawa.pet.tab.status",
        "screen.chiikawa.pet.tab.orders",
        "screen.chiikawa.pet.no_slip",
        "screen.chiikawa.pet.eager",
        "screen.chiikawa.pet.money",
        "screen.chiikawa.pet.gift",
        "screen.chiikawa.pet.gift_hint",
        "screen.chiikawa.pet.order.follow",
        "screen.chiikawa.pet.order.follow.hint",
        "screen.chiikawa.pet.order.stay",
        "screen.chiikawa.pet.order.stay.hint",
        "screen.chiikawa.pet.order.free",
        "screen.chiikawa.pet.order.free.hint",
        "block.chiikawa.labor_board",
        "screen.chiikawa.settings.title",
        "screen.chiikawa.settings.wild_spawn",
        "screen.chiikawa.settings.chatter",
        "screen.chiikawa.settings.speech_bubbles",
        "screen.chiikawa.settings.on",
        "screen.chiikawa.settings.off",
        "screen.chiikawa.settings.world_only",
        "screen.chiikawa.settings.read_only",
        "screen.chiikawa.settings.volume",
        "item.chiikawa.camera",
        "tooltip.chiikawa.camera",
        "item.chiikawa.film",
        "tooltip.chiikawa.film",
        "screen.chiikawa.album.caption",
        "screen.chiikawa.album.caption.scenery",
        "tooltip.chiikawa.film.shots",
        "screen.chiikawa.album.loading",
        "screen.chiikawa.album.missing",
        "screen.chiikawa.album.previous_page",
        "screen.chiikawa.album.next_page"
    };

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void everyIntentHasAName() {
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = collect(locale);
            for (PetIntent intent : PetIntents.all()) {
                String key = "intent." + intent.id().getNamespace() + "." + intent.id().getPath();
                assertTrue(translations.containsKey(key), () -> locale + " is missing " + key);
            }
        }
    }

    @Test
    void everySlipTypeHasAName() {
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = collect(locale);
            for (Identifier type : List.of(PetTaskTypeData.WEEDING, PetTaskTypeData.STREET_PERFORMANCE,
                    PetTaskTypeData.MELEE_HUNTING, PetTaskTypeData.RANGED_HUNTING)) {
                String key = "pet_task." + type.getNamespace() + "." + type.getPath();
                assertTrue(translations.containsKey(key), () -> locale + " is missing " + key);
                assertTrue(translations.containsKey(key + ".amount"), () -> locale + " is missing " + key + ".amount");
            }
        }
    }

    @Test
    void everyScreenLineHasATranslation() {
        for (String locale : LanguageData.LOCALES) {
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
