package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.settings.WorldSettings;
import com.dwinovo.chiikawa.testing.MinecraftFont;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Every slider of the settings screen has its words in the groove it is drawn in, in every language, measured with the game's
 * own font: the words are drawn across the groove and cut nowhere, so one that is too wide runs under the knob. Measured at the
 * widest the number can be, which is the most the setting can be turned up to.
 */
class SettingsLabelsTest {
    private static final Map<String, Float> SLIDERS = Map.of(
        "screen.chiikawa.settings.wild_spawn", 1.0F,
        "screen.chiikawa.settings.chatter", WorldSettings.MAX_CHATTER,
        "screen.chiikawa.settings.pastimes", WorldSettings.MAX_PASTIMES);

    @Test
    void everySlidersWordsFitTheGrooveAtTheirWidest() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = new HashMap<>();
            LanguageData.addTranslations(locale, words::put);
            SLIDERS.forEach((key, most) -> {
                String label = words.get(key);
                if (label == null) {
                    tooWide.add(locale + " has no " + key);
                    return;
                }
                String shown = String.format(Locale.ROOT, label, Math.round(most * 100) + "%");
                int width = MinecraftFont.width(shown);
                if (width > SettingsScreen.SLIDER_TEXT_W) {
                    tooWide.add(locale + " " + key + " \"" + shown + "\" is " + width + " pixels, the groove has "
                        + SettingsScreen.SLIDER_TEXT_W);
                }
            });
        }
        assertEquals(List.of(), tooWide);
    }
}
