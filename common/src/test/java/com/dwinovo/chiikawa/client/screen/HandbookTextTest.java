package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.testing.MinecraftFont;
import com.dwinovo.chiikawa.ui.TextWrap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Every chapter of the handbook is read in full, in every language, on a window with the room: the text is broken into lines
 * as the screen breaks it, with the game's own font, and has to leave the scene its smallest height without the wheel. A
 * chapter that does not is not cut, the screen scrolls it, but a handbook that has to be scrolled for its first
 * paragraph is one nobody reads to the end.
 */
class HandbookTextTest {
    private static final String CHAPTER = "manual.chiikawa.";
    private static final String TEXT = ".text";

    @Test
    void everyChapterFitsItsPageWithoutScrolling() {
        int room = HandbookLayout.linesWithoutScrolling(HandbookLayout.MAX_HEIGHT);
        List<String> tooLong = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            List<String> keys = words.keySet().stream().filter(key -> key.startsWith(CHAPTER) && key.endsWith(TEXT)).sorted().toList();
            assertFalse(keys.isEmpty(), "no chapter has a text in " + locale);
            for (String key : keys) {
                int lines = TextWrap.wrap(words.get(key), HandbookLayout.TEXT_W, MinecraftFont::width).size();
                if (lines > room) {
                    tooLong.add(locale + " " + key + " is " + lines + " lines, the page has room for " + room);
                }
            }
        }
        assertEquals(List.of(), tooLong);
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
