package com.dwinovo.chiikawa.entity.brain.errand.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandFiles;
import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
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

        assertEquals(Set.of(WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER, WorkData.WEED, WorkData.PICK_MUSHROOM, WorkData.MELEE,
            WorkData.RANGED, WorkData.HUNT_MELEE, WorkData.HUNT_RANGED, WorkData.PLAY_MUSIC), loaded.works().keySet());
        assertEquals(ErrandSettingsData.all(), loaded.settings());
        assertTrue(loaded.errors().isEmpty() && loaded.warnings().isEmpty());
        assertInstanceOf(WorkErrand.class, Errands.get(WorkData.PLAY_MUSIC));
    }

    @Test
    void anErrandIsGoneWithItsFileAndSoAreItsSettings() {
        try {
            Errands.install(Map.of(), Map.of());

            assertNull(Errands.get(WorkData.PLAY_MUSIC));
            assertEquals(Optional.empty(), ErrandSettings.of(WorkData.PLAY_MUSIC));
            assertEquals(16, Errands.all().size(), "the code errands stay");
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
        file.add("target", foe("no_attack_target", "self"));

        assertSwitchedOff(file, "busk takes no target");
    }

    @Test
    void aFightInDefenceFileNamesTheGroundsOfDefenceAndNoCounterAndReadsBackAsItWasWritten() {
        for (ResourceLocation errand : new ResourceLocation[] {WorkData.MELEE, WorkData.RANGED}) {
            JsonObject file = ErrandFiles.all().get(errand).deepCopy().getAsJsonObject();

            assertEquals(foe("no_attack_target", "self", "owner", "nearby"), file.get("target"), errand::toString);
            assertEquals("combat", file.get("category").getAsString(), errand::toString);
            assertEquals(new JsonArray(), file.get("requires"), errand::toString);
            // The slay is the death hook's, and a fight at this priority has no use for the slip bonus on top of it.
            assertFalse(file.has("counter"), errand::toString);
            assertEquals(80, file.get("priority").getAsInt(), errand::toString);

            WorkFile<?, ?> read = WorkFile.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();

            assertEquals(WorkData.all().get(errand), read.work(), errand::toString);
            assertEquals(new WorkTarget.Foe("no_attack_target", Set.of(Ground.SELF, Ground.OWNER, Ground.NEARBY)),
                read.work().target().orElseThrow(), errand::toString);
            assertEquals(file, WorkFile.CODEC.encodeStart(JsonOps.INSTANCE, read).getOrThrow(), errand::toString);
        }
    }

    @Test
    void aHuntFileNamesTheHuntAndTheSlayAndNothingElseNamesTheSlay() {
        for (ResourceLocation errand : new ResourceLocation[] {WorkData.HUNT_MELEE, WorkData.HUNT_RANGED}) {
            JsonObject file = ErrandFiles.all().get(errand).deepCopy().getAsJsonObject();

            assertEquals(foe("no_attack_target", "hunt"), file.get("target"), errand::toString);
            assertEquals("combat", file.get("category").getAsString(), errand::toString);
            assertEquals(new JsonArray(), file.get("requires"), errand::toString);
            assertEquals("chiikawa:slay", file.get("counter").getAsString(), errand::toString);
            assertEquals(50, file.get("priority").getAsInt(), errand::toString);

            WorkFile<?, ?> read = WorkFile.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();

            assertEquals(WorkData.all().get(errand), read.work(), errand::toString);
            assertEquals(new WorkTarget.Foe("no_attack_target", Set.of(Ground.HUNT)), read.work().target().orElseThrow(),
                errand::toString);
            assertEquals(file, WorkFile.CODEC.encodeStart(JsonOps.INSTANCE, read).getOrThrow(), errand::toString);
        }

    }

    @Test
    void theFarmFilesNameTheirTargetsAndReadBackAsTheyWereWritten() {
        Map<ResourceLocation, JsonObject> targets = Map.of(
            WorkData.HARVEST, target("ripe_crop", "no_crop"),
            WorkData.PLANT, target("plantable_soil", "no_farmland"),
            WorkData.DELIVER, container("no_container"),
            WorkData.WEED, blocks("#chiikawa:entity_weeds", "no_weed"),
            WorkData.PICK_MUSHROOM, blocks("#chiikawa:entity_mushrooms", "no_mushroom"));

        targets.forEach((errand, target) -> {
            JsonObject file = ErrandFiles.all().get(errand).deepCopy().getAsJsonObject();

            assertEquals(target, file.get("target"), errand::toString);
            WorkFile<?, ?> read = WorkFile.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();

            assertEquals(WorkData.all().get(errand), read.work(), errand::toString);
            assertEquals(file, WorkFile.CODEC.encodeStart(JsonOps.INSTANCE, read).getOrThrow(), errand::toString);
        });
    }

    @Test
    void theFarmFilesKeepWhatTheFarmErrandsAlwaysHad() {
        Map<ResourceLocation, JsonObject> files = new HashMap<>();
        for (ResourceLocation errand : WorkData.all().keySet()) {
            files.put(errand, ErrandFiles.all().get(errand).deepCopy().getAsJsonObject());
        }

        assertEquals("chiikawa:harvest_block", files.get(WorkData.HARVEST).get("action").getAsString());
        assertEquals("chiikawa:plant_block", files.get(WorkData.PLANT).get("action").getAsString());
        assertEquals("chiikawa:deposit", files.get(WorkData.DELIVER).get("action").getAsString());
        assertEquals("chiikawa:harvest_block", files.get(WorkData.WEED).get("action").getAsString());
        assertEquals("chiikawa:harvest_block", files.get(WorkData.PICK_MUSHROOM).get("action").getAsString());
        for (ResourceLocation errand : new ResourceLocation[] {WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER}) {
            assertEquals("work", files.get(errand).get("category").getAsString(), errand::toString);
            assertFalse(files.get(errand).has("counter"), errand::toString);
            assertEquals(new JsonArray(), files.get(errand).get("requires"), errand::toString);
        }
        for (ResourceLocation errand : new ResourceLocation[] {WorkData.WEED, WorkData.PICK_MUSHROOM}) {
            assertEquals("forage", files.get(errand).get("category").getAsString(), errand::toString);
            assertEquals(errand.toString(), files.get(errand).get("counter").getAsString(), errand::toString);
        }
        assertEquals(strings("wild_or_slip"), files.get(WorkData.WEED).get("requires"));
        assertEquals(strings("wild_or_slip", "at_night"), files.get(WorkData.PICK_MUSHROOM).get("requires"));
        // Only the reaping wears the tool; planting has no such number to set.
        assertEquals(1, tuning(files, WorkData.HARVEST).get("tool_wear").getAsInt());
        assertEquals(0, tuning(files, WorkData.WEED).get("tool_wear").getAsInt());
        assertEquals(0, tuning(files, WorkData.PICK_MUSHROOM).get("tool_wear").getAsInt());
        assertFalse(tuning(files, WorkData.PLANT).has("tool_wear"));
    }

    @Test
    void eachActionRefusesTheTargetsItDoesNotTake() {
        Map<String, JsonObject> targets = Map.of(
            "foe", foe("no_attack_target", "self"),
            "ripe_crop", target("ripe_crop", "no_crop"),
            "plantable_soil", target("plantable_soil", "no_farmland"),
            "blocks", blocks("#chiikawa:entity_weeds", "no_weed"),
            "container", container("no_container"));
        Map<ResourceLocation, Set<String>> taken = Map.of(
            WorkData.MELEE, Set.of("foe"),
            WorkData.RANGED, Set.of("foe"),
            WorkData.HUNT_MELEE, Set.of("foe"),
            WorkData.HUNT_RANGED, Set.of("foe"),
            WorkData.HARVEST, Set.of("ripe_crop", "blocks"),
            WorkData.PLANT, Set.of("plantable_soil"),
            WorkData.DELIVER, Set.of("container"),
            WorkData.PLAY_MUSIC, Set.of());

        taken.forEach((errand, kinds) -> targets.forEach((kind, target) -> {
            JsonObject file = ErrandFiles.all().get(errand).deepCopy().getAsJsonObject();
            file.add("target", target);

            Map<ResourceLocation, JsonElement> files = new HashMap<>(ErrandFiles.all());
            files.put(errand, file);
            Loaded loaded = ErrandSettingsLoader.load(files);
            String what = errand + " given a " + kind;
            if (kinds.contains(kind)) {
                assertTrue(loaded.works().containsKey(errand), what);
                assertTrue(loaded.errors().isEmpty(), what);
            } else {
                assertFalse(loaded.works().containsKey(errand), what);
                // What yields to it is switched off with it, so it is the refusal of its own file that is looked for.
                List<String> refusals = loaded.errors().stream().filter(error -> error.contains(errand + " is switched off")).toList();
                assertEquals(1, refusals.size(), what);
                assertTrue(refusals.get(0).contains("not a " + kind) || refusals.get(0).contains("takes no target"), refusals.get(0));
            }
        }));
    }

    @Test
    void aBlockWorkWithoutATargetSwitchesTheFileOff() {
        Map<ResourceLocation, String> refusal = Map.of(
            WorkData.HARVEST, "harvest_block needs a ripe_crop or blocks target",
            WorkData.PLANT, "plant_block needs a plantable_soil target",
            WorkData.DELIVER, "deposit needs a container target");

        refusal.forEach((errand, because) -> {
            JsonObject file = ErrandFiles.all().get(errand).deepCopy().getAsJsonObject();
            file.remove("target");

            assertSwitchedOff(errand, file, because);
        });
    }

    @Test
    void aBlockTargetNeedsItsReasonAndItsTagsWrittenAsTags() {
        JsonObject file = ErrandFiles.all().get(WorkData.WEED).deepCopy().getAsJsonObject();
        file.get("target").getAsJsonObject().remove("missing");
        assertSwitchedOff(WorkData.WEED, file, "missing");

        file = ErrandFiles.all().get(WorkData.WEED).deepCopy().getAsJsonObject();
        file.get("target").getAsJsonObject().addProperty("blocks", "chiikawa:entity_weeds");
        assertSwitchedOff(WorkData.WEED, file, "tag id");

        file = ErrandFiles.all().get(WorkData.DELIVER).deepCopy().getAsJsonObject();
        file.get("target").getAsJsonObject().remove("items");
        assertSwitchedOff(WorkData.DELIVER, file, "items");
    }

    @Test
    void aFightWithoutATargetSwitchesTheFileOff() {
        for (ResourceLocation errand : new ResourceLocation[] {WorkData.MELEE, WorkData.RANGED, WorkData.HUNT_MELEE,
                WorkData.HUNT_RANGED}) {
            JsonObject file = ErrandFiles.all().get(errand).deepCopy().getAsJsonObject();
            file.remove("target");

            assertSwitchedOff(errand, file, errand.equals(WorkData.MELEE) || errand.equals(WorkData.HUNT_MELEE)
                ? "strike needs a foe target" : "shoot needs a foe target");
        }
    }

    @Test
    void aFoeTargetWithNoGroundsSwitchesTheFileOff() {
        JsonObject file = ErrandFiles.all().get(WorkData.HUNT_MELEE).deepCopy().getAsJsonObject();
        file.get("target").getAsJsonObject().add("grounds", new JsonArray());
        assertSwitchedOff(WorkData.HUNT_MELEE, file, "List must have contents");

        file = ErrandFiles.all().get(WorkData.HUNT_MELEE).deepCopy().getAsJsonObject();
        file.get("target").getAsJsonObject().remove("grounds");
        assertSwitchedOff(WorkData.HUNT_MELEE, file, "grounds");

        file = ErrandFiles.all().get(WorkData.HUNT_MELEE).deepCopy().getAsJsonObject();
        file.get("target").getAsJsonObject().add("grounds", strings("the_moon"));
        assertSwitchedOff(WorkData.HUNT_MELEE, file, "the_moon");
    }

    @Test
    void aFoeTargetWithoutItsReasonSwitchesTheFileOff() {
        JsonObject file = ErrandFiles.all().get(WorkData.MELEE).deepCopy().getAsJsonObject();
        file.get("target").getAsJsonObject().remove("missing");

        assertSwitchedOff(WorkData.MELEE, file, "missing");
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
        Loaded loaded = ErrandSettingsLoader.load(Map.of(ErrandIds.WANDER, playMusic()));

        assertTrue(loaded.works().isEmpty() && loaded.settings().isEmpty());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("chiikawa:wander") && loaded.errors().get(0).contains("written in code"),
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
        assertSwitchedOff(WorkData.PLAY_MUSIC, file, because);
    }

    private static void assertSwitchedOff(ResourceLocation errand, JsonElement file, String because) {
        Loaded loaded = ErrandSettingsLoader.load(Map.of(errand, file));

        assertTrue(loaded.works().isEmpty() && loaded.settings().isEmpty(), "still on");
        assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        assertTrue(loaded.errors().get(0).contains(because), loaded.errors().get(0));
        assertTrue(loaded.errors().get(0).contains(errand.toString()) && loaded.errors().get(0).contains("switched off"),
            loaded.errors().get(0));
    }

    private static JsonObject target(String type, String missing) {
        JsonObject target = new JsonObject();
        target.addProperty("type", type);
        target.addProperty("missing", missing);
        return target;
    }

    private static JsonObject blocks(String tag, String missing) {
        JsonObject target = new JsonObject();
        target.addProperty("type", "blocks");
        target.addProperty("blocks", tag);
        target.addProperty("missing", missing);
        return target;
    }

    private static JsonObject container(String missing) {
        JsonObject target = new JsonObject();
        target.addProperty("type", "container");
        target.addProperty("blocks", "#chiikawa:entity_delever_container");
        target.addProperty("items", "#chiikawa:entity_deliver_items");
        target.addProperty("missing", missing);
        return target;
    }

    private static JsonArray strings(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonObject tuning(Map<ResourceLocation, JsonObject> files, ResourceLocation errand) {
        return files.get(errand).getAsJsonObject("tuning");
    }

    private static JsonObject foe(String missing, String... grounds) {
        JsonObject foe = target("foe", missing);
        foe.add("grounds", strings(grounds));
        return foe;
    }

    /** @return the file the data generator writes for playing music, to spoil */
    private static JsonObject playMusic() {
        return ErrandFiles.all().get(WorkData.PLAY_MUSIC).deepCopy().getAsJsonObject();
    }
}
