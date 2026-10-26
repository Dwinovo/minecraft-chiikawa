package com.dwinovo.chiikawa.entity.brain.errand.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandFiles;
import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettingsLoader;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettingsLoader.Loaded;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** An errand file with an action is a work: what it says, how it reads back, and what switches its errand off. */
class ErrandFileTest {
    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void aWorkFileHasTheWorkBesideTheSettingsAndReadsBackAsItWasWritten() {
        JsonObject file = playMusic();

        assertEquals("chiikawa:busk", file.get("action").getAsString());
        assertEquals("work", file.get("category").getAsString());
        assertEquals("chiikawa:play_music_second", file.get("counter").getAsString());
        assertEquals(new JsonArray(), file.get("requires"));
        assertEquals(Set.of("action", "category", "counter", "requires", "priority", "score", "max_ticks", "rests",
            "yields_to", "overlays", "tuning"), file.keySet());

        WorkFile<?, ?> read = WorkFile.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();

        assertEquals(WorkData.all().get(WorkData.PLAY_MUSIC), read.work());
        assertEquals(ErrandSettingsData.all().get(WorkData.PLAY_MUSIC), read.settings());
        assertEquals(file, WorkFile.CODEC.encodeStart(JsonOps.INSTANCE, read).getOrThrow());
    }

    @Test
    void aFileWithAnActionMakesTheWorkErrandAndAFileWithoutOneIsACodeErrandsSettings() {
        Loaded loaded = ErrandSettingsLoader.load(ErrandFiles.all());

        assertEquals(Set.of(WorkData.PLAY_MUSIC), loaded.works().keySet());
        assertEquals(ErrandSettingsData.all(), loaded.settings());
        assertTrue(loaded.errors().isEmpty() && loaded.warnings().isEmpty());
        assertInstanceOf(WorkErrand.class, Errands.get(WorkData.PLAY_MUSIC));
        assertEquals(21, Errands.all().size());
    }

    @Test
    void anErrandIsGoneWithItsFileAndSoAreItsSettings() {
        try {
            Errands.install(Map.of(), Map.of());

            assertNull(Errands.get(WorkData.PLAY_MUSIC));
            assertEquals(Optional.empty(), ErrandSettings.of(WorkData.PLAY_MUSIC));
            assertEquals(20, Errands.all().size(), "the code errands stay");
        } finally {
            TestErrands.load();
        }
    }

    @Test
    void anActionNobodyWroteSwitchesTheFileOff() {
        JsonObject file = playMusic();
        file.addProperty("action", "chiikawa:sing");

        assertSwitchedOff(file, "chiikawa:sing is no work action");
    }

    @Test
    void aTargetTheActionDoesNotTakeSwitchesTheFileOff() {
        JsonObject file = playMusic();
        JsonObject target = new JsonObject();
        target.addProperty("type", "foe");
        file.add("target", target);

        assertSwitchedOff(file, "busk takes no target");
    }

    @Test
    void aTargetOfNoKindSwitchesTheFileOff() {
        JsonObject file = playMusic();
        JsonObject target = new JsonObject();
        target.addProperty("type", "moon");
        file.add("target", target);

        assertSwitchedOff(file, "moon");
    }

    @Test
    void aRequirementOfNoKindSwitchesTheFileOff() {
        JsonObject file = playMusic();
        JsonArray requires = new JsonArray();
        requires.add("when_it_rains");
        file.add("requires", requires);

        assertSwitchedOff(file, "when_it_rains");
    }

    @Test
    void wildOrSlipWithoutACounterSwitchesTheFileOffAndWithOneDoesNot() {
        JsonObject file = playMusic();
        JsonArray requires = new JsonArray();
        requires.add("wild_or_slip");
        file.add("requires", requires);

        Loaded loaded = ErrandSettingsLoader.load(Map.of(WorkData.PLAY_MUSIC, file));

        assertEquals(Set.of(WorkData.PLAY_MUSIC), loaded.works().keySet());
        assertTrue(loaded.errors().isEmpty());

        file.remove("counter");
        assertSwitchedOff(file, "needs a counter");
    }

    @Test
    void aCategoryOfNoKindSwitchesTheFileOff() {
        JsonObject file = playMusic();
        file.addProperty("category", "dancing");

        assertSwitchedOff(file, "dancing");
        file.remove("category");
        assertSwitchedOff(file, "category");
    }

    @Test
    void aWorkFileUnderTheIdOfACodeErrandSwitchesThatErrandOff() {
        Loaded loaded = ErrandSettingsLoader.load(Map.of(ErrandIds.HARVEST, playMusic()));

        assertTrue(loaded.works().isEmpty() && loaded.settings().isEmpty());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("chiikawa:harvest") && loaded.errors().get(0).contains("written in code"),
            loaded.errors().get(0));
    }

    @Test
    void aWorkFileKeepsTheChecksOfEveryErrandFile() {
        JsonObject file = playMusic();
        JsonArray yields = new JsonArray();
        yields.add("chiikawa:harvest");
        file.add("yields_to", yields);

        assertSwitchedOff(file, "yields_to");

        file = playMusic();
        file.addProperty("max_ticks", 0);
        assertSwitchedOff(file, "chiikawa:play_music");
    }

    @Test
    void aFileWithoutAnActionNamedForNoCodeErrandIsOnlyWarnedAbout() {
        JsonObject file = playMusic();
        file.remove("action");

        Loaded loaded = ErrandSettingsLoader.load(Map.of(WorkData.PLAY_MUSIC, file));

        assertTrue(loaded.works().isEmpty() && loaded.settings().isEmpty() && loaded.errors().isEmpty());
        assertEquals(1, loaded.warnings().size());
        assertTrue(loaded.warnings().get(0).contains("chiikawa:play_music"), loaded.warnings().get(0));
    }

    @Test
    void aWorkWithNoCounterAndNoRequirementsNeedsNeither() {
        JsonObject file = playMusic();
        file.remove("counter");
        file.remove("requires");

        Loaded loaded = ErrandSettingsLoader.load(Map.of(WorkData.PLAY_MUSIC, file));

        assertEquals(Set.of(WorkData.PLAY_MUSIC), loaded.works().keySet());
        assertTrue(loaded.errors().isEmpty());
        assertEquals(Optional.empty(), loaded.works().get(WorkData.PLAY_MUSIC).workCounter());
        assertEquals(ErrandCategory.WORK, loaded.works().get(WorkData.PLAY_MUSIC).category());
    }

    private static void assertSwitchedOff(JsonElement file, String because) {
        Loaded loaded = ErrandSettingsLoader.load(Map.of(WorkData.PLAY_MUSIC, file));

        assertTrue(loaded.works().isEmpty() && loaded.settings().isEmpty(), "still on");
        assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        assertTrue(loaded.errors().get(0).contains(because), loaded.errors().get(0));
        assertTrue(loaded.errors().get(0).contains("chiikawa:play_music") && loaded.errors().get(0).contains("switched off"),
            loaded.errors().get(0));
    }

    /** @return the file the data generator writes for playing music, to spoil */
    private static JsonObject playMusic() {
        return ErrandFiles.all().get(WorkData.PLAY_MUSIC).deepCopy().getAsJsonObject();
    }
}
