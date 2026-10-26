package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkActions;
import com.dwinovo.chiikawa.testing.MinecraftFont;
import com.dwinovo.chiikawa.ui.TextWrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Every word drawn on the cards of the status page fits the room the screen draws them in, in every language, measured with
 * the game's own font: what the pet would do next, why it is not at work, and what it is short of, which is every lack an action says it can report ({@code WorkAction#lacks}). The cards break a long line
 * in two as the screen does and cut what is longer with an ellipsis, so what is checked here is that nothing is ever cut. The
 * questions under the cursor are not here, which have the room of the screen.
 */
class StatusLabelsTest {
    private static final String NAME = "intent.chiikawa.";
    private static final String REASON = "intent.chiikawa.fail.";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void whatThePetWouldDoNextFitsItsCard() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            Map<String, String> names = withPrefix(words, NAME, REASON);
            assertFalse(names.isEmpty(), "no errand has a name in " + locale);
            names.forEach((key, text) -> fits(tooWide, locale, key, text));
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void everyReasonAPetIsNotAtWorkFitsItsCard() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> reasons = withPrefix(collect(locale), REASON);
            assertFalse(reasons.isEmpty(), "no reason is given in " + locale);
            reasons.forEach((key, text) -> fits(tooWide, locale, key, text));
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void whatThePetIsShortOfFitsItsCardWithTheWordsForNeedingIt() {
        List<LackKey> lacks = WorkActions.all().stream().flatMap(action -> action.lacks().stream()).toList();
        assertFalse(lacks.isEmpty(), "no action names what a pet can be short of");
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            String needs = words.get("screen.chiikawa.pet.outlook.lacks");
            for (LackKey lack : lacks) {
                String name = words.get(lack.translationKey());
                if (needs == null || name == null) {
                    tooWide.add(locale + " has no words for " + (needs == null ? "needing" : lack.translationKey()));
                } else {
                    fits(tooWide, locale, lack.translationKey(), String.format(Locale.ROOT, needs, name));
                }
            }
        }
        assertEquals(List.of(), tooWide);
    }

    /** Notes what takes more lines than the card has, so that one run shows all of it. */
    private static void fits(List<String> tooWide, String locale, String key, String text) {
        List<String> lines = TextWrap.wrap(text, StatusLayout.ROW_TEXT_W, MinecraftFont::width);
        if (lines.size() > StatusLayout.ROW_LINES) {
            tooWide.add(locale + " " + key + " \"" + text + "\" takes " + lines.size() + " lines of " + StatusLayout.ROW_TEXT_W
                + " pixels, the card has " + StatusLayout.ROW_LINES);
        }
    }

    /** @return the translations whose key starts with the prefix and not with the other */
    private static Map<String, String> withPrefix(Map<String, String> words, String prefix, String... except) {
        Map<String, String> found = new TreeMap<>();
        words.forEach((key, text) -> {
            if (key.startsWith(prefix) && Arrays.stream(except).noneMatch(key::startsWith)) {
                found.put(key, text);
            }
        });
        return found;
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
