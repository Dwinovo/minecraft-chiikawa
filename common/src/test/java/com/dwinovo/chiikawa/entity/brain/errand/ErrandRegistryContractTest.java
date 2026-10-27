package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What every errand has, so that none can be added without it: data that decodes, a fixture of a pet
 * that has something to offer it, and a name in every language.
 */
class ErrandRegistryContractTest {
    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void thereAreTheThirtyErrandsOfThisVersion() {
        assertEquals(30, Errands.all().size());
        assertEquals(Errands.all().size(), new HashSet<>(Errands.all().stream().map(ErrandType::id).toList()).size(), "each id once");
        assertTrue(Errands.all().containsAll(Errands.generic()));
    }

    @Test
    void everyErrandHasSettingsThatDecodeAndNothingElseDoes() {
        Map<ResourceLocation, ErrandSettings<?>> data = ErrandSettingsData.all();

        assertEquals(Errands.all().stream().map(ErrandType::id).collect(Collectors.toSet()), data.keySet());
        Errands.all().forEach(errand -> {
            ErrandSettings<?> settings = data.get(errand.id());
            var json = ErrandSettings.codecOf(errand.id()).encodeStart(JsonOps.INSTANCE, settings).getOrThrow();

            assertEquals(settings, ErrandSettings.codecOf(errand.id()).parse(JsonOps.INSTANCE, json).getOrThrow(), errand.id().toString());
            assertTrue(settings.maxTicks() > 0, errand.id().toString());
            assertTrue(settings.rests().gaveUp().lasts(), errand.id() + " must rest after giving up");
        });
    }

    @Test
    void everyErrandHasAFixtureAndOffersSomethingTo() {
        Errands.all().forEach(errand -> {
            assertTrue(ErrandFixtures.of(errand.id()).isPresent(), errand.id() + " has no fixture in ErrandFixtures");
            assertFalse(TestErrands.offers(errand.id(), ErrandFixtures.of(errand.id()).orElseThrow()).isEmpty(),
                errand.id() + " offers nothing to its own fixture");
        });
    }

    @Test
    void everyErrandHasANameInEveryLanguage() {
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = new HashMap<>();
            LanguageData.addTranslations(locale, translations::put);
            for (ErrandType<?, ?> errand : Errands.all()) {
                String key = "intent." + errand.id().getNamespace() + "." + errand.id().getPath();
                assertTrue(translations.containsKey(key), () -> locale + " is missing " + key);
            }
        }
    }
}
