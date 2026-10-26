package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.entity.CombatStance;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.testing.MinecraftFont;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Every word drawn on the orders page fits the box the screen draws it in, in every language, measured with the game's own
 * font: the screen cuts what does not fit with an ellipsis, and a stance that reads "Hunt fo..." is a stance nobody
 * understands. The tooltips are not here, which have the room of the screen.
 */
class OrdersLabelsTest {
    @Test
    void anOrdersNameAndItsHintFitTheCard() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            for (PetDirective order : PetDirective.values()) {
                String key = "screen.chiikawa.pet.order." + order.name().toLowerCase(Locale.ROOT);
                fits(tooWide, locale, words, key, OrdersLayout.ORDER_TEXT_W);
                fits(tooWide, locale, words, key + ".hint", OrdersLayout.ORDER_TEXT_W);
            }
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void aStancesNameFitsItsCard() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            for (CombatStance stance : CombatStance.values()) {
                fits(tooWide, locale, words, "screen.chiikawa.pet.stance." + stance.getSerializedName(),
                    OrdersLayout.STANCE_TEXT_W);
            }
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void theWeaponSwapsNameFitsItsCard() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            fits(tooWide, locale, collect(locale), "screen.chiikawa.pet.swap", OrdersLayout.SWAP_TEXT_W);
        }
        assertEquals(List.of(), tooWide);
    }

    /** Notes what is too wide for its box, so that one run shows all of it. */
    private static void fits(List<String> tooWide, String locale, Map<String, String> words, String key, int room) {
        String text = words.get(key);
        if (text == null) {
            tooWide.add(locale + " has no " + key);
            return;
        }
        int width = MinecraftFont.width(text);
        if (width > room) {
            tooWide.add(locale + " " + key + " \"" + text + "\" is " + width + " wide, the box is " + room);
        }
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
