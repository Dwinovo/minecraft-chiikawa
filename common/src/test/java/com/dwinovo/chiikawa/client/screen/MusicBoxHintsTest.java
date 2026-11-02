package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.testing.MinecraftFont;
import com.dwinovo.chiikawa.ui.TextWrap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Every line the music box writes in its list, in every language, measured with the game's own font: the catalog line and
 * the format hint are drawn on one line, centred and cut nowhere, so one wider than the panel's text runs out over its
 * edge. The two hints on what to do about an empty list are broken into lines, and have to fit the few the list has room for.
 */
class MusicBoxHintsTest {
    private static final List<String> ONE_LINE = List.of(
        "screen.chiikawa.music_box.empty_catalog",
        "screen.chiikawa.music_box.format_hint");
    private static final List<String> WRAPPED = List.of(
        "screen.chiikawa.music_box.empty_hint",
        "screen.chiikawa.music_box.empty_hint_server");

    @Test
    void everyHintFitsThePanelInEveryLanguage() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = new HashMap<>();
            LanguageData.addTranslations(locale, words::put);
            for (String key : WRAPPED) {
                String hint = words.get(key);
                if (hint == null) {
                    tooWide.add(locale + " has no " + key);
                    continue;
                }
                int lines = TextWrap.wrap(hint, MusicBoxScreen.HINT_W, MinecraftFont::width).size();
                if (lines > MusicBoxScreen.HINT_LINES) {
                    tooWide.add(locale + " " + key + " \"" + hint + "\" is " + lines + " lines, the list has room for "
                        + MusicBoxScreen.HINT_LINES);
                }
            }
            for (String key : ONE_LINE) {
                String hint = words.get(key);
                if (hint == null) {
                    tooWide.add(locale + " has no " + key);
                    continue;
                }
                int width = MinecraftFont.width(hint);
                if (width > MusicBoxScreen.HINT_W) {
                    tooWide.add(locale + " " + key + " \"" + hint + "\" is " + width + " pixels, the panel has "
                        + MusicBoxScreen.HINT_W);
                }
            }
        }
        assertEquals(List.of(), tooWide);
    }
}
