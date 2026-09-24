package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.voice.PetVoice;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What the pets say is two halves kept in two places: which lines there are, in the voice
 * data, and their words, in the language files. These hold the halves together.
 */
class LanguageDataVoiceTranslationTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void everyLineAPetCanSayHasWordsInEveryLanguage() {
        Set<String> said = lineKeys();
        assertFalse(said.isEmpty(), "nobody says anything");
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = collect(locale);
            for (String key : said) {
                assertTrue(translations.containsKey(key), () -> locale + " is missing " + key);
                assertFalse(translations.get(key).isBlank(), () -> locale + " leaves " + key + " blank");
            }
        }
    }

    @Test
    void everyLineWrittenIsOneAPetCanSay() {
        Set<String> written = collect("en_us").keySet().stream()
            .filter(key -> key.startsWith("voice."))
            .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(new TreeSet<>(lineKeys()), written);
    }

    @Test
    void everyVoiceBelongsToAPetTheGameKnowsByName() {
        Map<String, String> english = collect("en_us");
        for (ResourceLocation pet : PetVoiceData.all().keySet()) {
            String key = "entity." + pet.getNamespace() + "." + pet.getPath();
            assertTrue(english.containsKey(key), () -> pet + " is not a pet");
        }
    }

    @Test
    void everyVoiceSurvivesTheTripThroughItsFile() {
        PetVoiceData.all().forEach((pet, voice) -> assertEquals(voice,
            PetVoice.CODEC.parse(JsonOps.INSTANCE, PetVoice.CODEC.encodeStart(JsonOps.INSTANCE, voice).getOrThrow())
                .getOrThrow(), pet::toString));
    }

    private static Set<String> lineKeys() {
        return PetVoiceData.all().values().stream()
            .flatMap(voice -> voice.lines().values().stream())
            .flatMap(lines -> lines.stream().map(PetVoice.Line::text))
            .collect(Collectors.toSet());
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
