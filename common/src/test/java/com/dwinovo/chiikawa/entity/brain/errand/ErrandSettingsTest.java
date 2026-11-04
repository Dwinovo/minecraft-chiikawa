package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettingsLoader.Loaded;
import com.dwinovo.chiikawa.entity.brain.errand.overlay.Overlays;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.util.StringRepresentable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The settings a pack gives an errand, and what the loader does with a file that is wrong. */
class ErrandSettingsTest {
    private static final String PHOTO = """
        { "priority": 40, "score": 0.45, "max_ticks": 1200, "yields_to": [], "overlays": [],
          "rests": { "done": 0, "gave_up": { "from": "chiikawa:camera_retry" },
                     "interrupted": 0,
                     "reports": { "taken": { "from": "chiikawa:photo_upload_wait" }, "stored": { "from": "chiikawa:camera_cooldown" },
                                  "failed": { "from": "chiikawa:camera_retry" } } },
          "tuning": { "speed": 0.7, "stand_back": 4, "hold_ticks": 20, "look_back_ticks": 60, "view_distance": 16.0,
                      "max_walk_ticks": 400, "no_way_ticks": 100 } }""";

    private static final String PLANT = """
        { "priority": 50, "score": 0.6, "max_ticks": 600, "yields_to": ["chiikawa:stay"], "overlays": [],
          "rests": { "done": 0, "gave_up": { "ticks": 200, "per": "subject" }, "interrupted": 0 },
          "tuning": { "speed": 0.8, "pickup_distance": 3.0, "max_walk_ticks": 600, "no_way_ticks": 0 } }""";

    private static final String TAKE_TASK = """
        { "priority": 50, "score": 0.7, "max_ticks": 1200, "yields_to": [], "overlays": [],
          "rests": { "done": 0, "gave_up": 600, "interrupted": 600 },
          "tuning": { "speed": 0.7, "arrive_distance": 2, "take_distance": 3.0 } }""";

    private static final String HARVEST = PLANT.replace("\"yields_to\": [\"chiikawa:stay\"]", "\"yields_to\": []")
        .replace("\"tuning\": { \"speed\": 0.8, \"pickup_distance\": 3.0, \"max_walk_ticks\": 600, \"no_way_ticks\": 0 }", "\"tuning\": {}");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void settingsParseFromTheGeneratedShape() {
        ErrandSettings<?> settings = parse(ErrandIds.TAKE_PHOTO, PHOTO).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(40, settings.priority());
        assertEquals(0.45F, settings.score());
        assertEquals(1200, settings.maxTicks());
        assertEquals(RestSpec.NONE, settings.rests().done());
        assertEquals(RestSpec.from(RestSources.CAMERA_RETRY), settings.rests().gaveUp());
        assertEquals(RestSpec.NONE, settings.rests().interrupted());
        assertEquals(Map.of("taken", RestSpec.from(RestSources.PHOTO_UPLOAD_WAIT), "stored", RestSpec.from(RestSources.CAMERA_COOLDOWN),
            "failed", RestSpec.from(RestSources.CAMERA_RETRY)), settings.rests().reports());
        assertEquals(new ErrandTunings.TakePhoto(0.7F, 4, 20, 60, 16.0, 400, 100), settings.tuning());
    }

    @Test
    void aFileMayHaveFieldsAlongsideTheSettings() {
        Codec<Pair<String, ErrandSettings<ErrandTunings.TakeTask>>> beside = Codec.mapPair(
            Codec.STRING.fieldOf("action"), ErrandSettings.mapCodec(ErrandTunings.TakeTask.CODEC)).codec();
        Pair<String, ErrandSettings<ErrandTunings.TakeTask>> parsed = beside.parse(JsonOps.INSTANCE,
            JsonParser.parseString(TAKE_TASK.replace("{ \"priority\"", "{ \"action\": \"busk\", \"priority\""))).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals("busk", parsed.getFirst());
        assertEquals(50, parsed.getSecond().priority());
    }

    @Test
    void aCategoryIsNamedInLowerCaseAndEachByItsOwnName() {
        assertEquals("pick_up", ErrandCategory.PICK_UP.getSerializedName());
        assertEquals(ErrandCategory.values().length,
            Arrays.stream(ErrandCategory.values()).map(StringRepresentable::getSerializedName).distinct().count());
    }

    @Test
    void theErrandsAPackHasAreTheFilesInItsErrandDirectory(@TempDir Path root) throws IOException {
        write(root.resolve("data/chiikawa/pet_errand/harvest.json"));
        write(root.resolve("data/chiikawa/pet_errand/take_photo.json"));
        write(root.resolve("data/other/pet_errand/sulk.json"));
        write(root.resolve("data/chiikawa/pet_errand/notes.txt"));
        write(root.resolve("data/chiikawa/pet_job/farmer.json"));

        try (MultiPackResourceManager manager = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(new PathPackResources("test", root, false)))) {
            assertEquals(Set.of(WorkData.HARVEST, ErrandIds.TAKE_PHOTO, new ResourceLocation("other", "sulk")),
                ErrandSettingsLoader.errandIds(manager));
        }
    }

    @Test
    void theErrandsAPackHasThatAreDoneToBlocksAreTheOnesWhoseFileNamesABlockTarget(@TempDir Path root) throws IOException {
        Path errands = root.resolve("data/chiikawa/pet_errand");
        Files.createDirectories(errands);
        Files.writeString(errands.resolve("harvest.json"), "{ \"target\": { \"type\": \"ripe_crop\", \"missing\": \"no_crop\" } }");
        Files.writeString(errands.resolve("melee.json"), "{ \"target\": { \"type\": \"foe\", \"missing\": \"no_attack_target\" } }");
        Files.writeString(errands.resolve("play_music.json"), "{ \"action\": \"chiikawa:busk\" }");
        Files.writeString(errands.resolve("take_photo.json"), "{ \"target\": 5 }");

        try (MultiPackResourceManager manager = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(new PathPackResources("test", root, false)))) {
            assertEquals(Set.of(WorkData.HARVEST), ErrandSettingsLoader.blockWorkIds(manager));
        }
    }

    @Test
    void settingsEncodeToWhatTheyParseFrom() {
        decoded(Map.of(ErrandIds.TAKE_PHOTO, PHOTO, ErrandIds.PICK_UP_ITEM, PLANT)).forEach((errand, settings) -> {
            JsonElement encoded = ErrandSettings.codecOf(errand).encodeStart(JsonOps.INSTANCE, settings).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

            assertEquals(settings, ErrandSettings.codecOf(errand).parse(JsonOps.INSTANCE, encoded).getOrThrow(false, org.junit.jupiter.api.Assertions::fail));
        });
    }

    @Test
    void theListsAndTheVoiceAreWrittenWhenTheyAreNone() {
        ErrandSettings<?> settings = parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("\"yields_to\": [], \"overlays\": [],", "")).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        var written = ErrandSettings.codecOf(ErrandIds.TAKE_PHOTO).encodeStart(JsonOps.INSTANCE, settings).getOrThrow(false, org.junit.jupiter.api.Assertions::fail).getAsJsonObject();

        assertEquals(0, written.getAsJsonArray("yields_to").size(), "a file without them reads as none, and they are written anyway");
        assertEquals(0, written.getAsJsonArray("overlays").size());
        assertFalse(written.has("start_voice"));
    }

    @Test
    void theVoiceOnSettingOffIsRead() {
        ErrandSettings<?> settings = parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"overlays\": [],", "\"overlays\": [], \"start_voice\": \"hunt\",")).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(Optional.of(VoiceMoment.HUNT), settings.startVoice());
    }

    @Test
    void anOverlayIsOneOfTheOnesTheModHasAndAnyOtherSwitchesTheErrandOff() {
        ErrandSettings<?> chatting = parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"overlays\": []", "\"overlays\": [\"chiikawa:idle_chatter\"]")).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(List.of(Overlays.IDLE_CHATTER), chatting.overlays());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"overlays\": []", "\"overlays\": [\"chiikawa:idle_chater\"]")).result().isEmpty());
    }

    @Test
    void aRestIsTicksTicksPerSubjectOrAnotherFilesNumber() {
        assertEquals(RestSpec.ticks(40), rest("40"));
        assertEquals(RestSpec.ticksPerSubject(40), rest("{ \"ticks\": 40, \"per\": \"subject\" }"));
        assertEquals(RestSpec.ticks(40), rest("{ \"ticks\": 40 }"), "per defaults to the errand");
        assertEquals(RestSpec.from(RestSources.SCENE_COOLDOWN), rest("{ \"from\": \"chiikawa:scene_cooldown\" }"));
        assertEquals(RestSpec.fromPerSubject(RestSources.SCENE_COOLDOWN),
            rest("{ \"from\": \"chiikawa:scene_cooldown\", \"per\": \"subject\" }"));
    }

    @Test
    void aRestIsWrittenAsShortAsItCanBe() {
        assertEquals("40", write(RestSpec.ticks(40)));
        assertEquals("{\"ticks\":40,\"per\":\"subject\"}", write(RestSpec.ticksPerSubject(40)));
        assertEquals("{\"from\":\"chiikawa:camera_retry\"}", write(RestSpec.from(RestSources.CAMERA_RETRY)));
    }

    @Test
    void aRestThatIsNotARestIsRejected() {
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("-1")).result().isEmpty(), "negative");
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"per\": \"subject\" }")).result().isEmpty(), "no amount");
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{ \"ticks\": 4, \"per\": \"world\" }")).result().isEmpty(), "per something else");
    }

    @Test
    void aNumberFromASourceThatDoesNotExistIsRejected() {
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{ \"from\": \"chiikawa:camera_retyr\" }")).result().isEmpty());
        assertTrue(parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("photo_upload_wait", "photo_upload_weight")).result().isEmpty());
        assertTrue(parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("\"stored\": { \"from\": \"chiikawa:camera_cooldown\" }",
            "\"stored\": { \"from\": \"chiikawa:nowhere\" }")).result().isEmpty(), "in the reports as well");
    }

    @Test
    void everyNamedSourceCanBePointedAt() {
        for (ResourceLocation name : RestSources.names()) {
            assertEquals(RestSpec.from(name), rest("{ \"from\": \"" + name + "\" }"));
        }
        assertEquals(7, RestSources.names().size());
    }

    @Test
    void anErrandThatGivesUpMustRestAfterwards() {
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("{ \"ticks\": 200, \"per\": \"subject\" }", "0")).result().isEmpty());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("{ \"ticks\": 200, \"per\": \"subject\" }",
            "{ \"ticks\": 0, \"per\": \"subject\" }")).result().isEmpty());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"gave_up\": { \"ticks\": 200, \"per\": \"subject\" }, ", "")).result().isEmpty(),
            "and say how long");
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("{ \"ticks\": 200, \"per\": \"subject\" }",
            "{ \"from\": \"chiikawa:camera_retry\" }")).result().isPresent(), "a source is taken to give something");
    }

    @Test
    void theOtherRestsMayBeNothing() {
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT).result().isPresent());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"done\": 0", "\"done\": 600")).result().isPresent());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"interrupted\": 0", "\"interrupted\": 600")).result().isPresent());
    }

    @Test
    void aRestAtBirthIsOptionalAndWrittenOnlyWhenThereIsOne() {
        ErrandSettings<?> without = parse(ErrandIds.PICK_UP_ITEM, PLANT).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
        ErrandSettings<?> with = parse(ErrandIds.PICK_UP_ITEM,
            PLANT.replace("\"interrupted\": 0", "\"interrupted\": 0, \"born\": { \"from\": \"chiikawa:pastime_cooldown\" }")).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);

        assertEquals(RestSpec.NONE, without.rests().born());
        assertEquals(RestSpec.from(RestSources.PASTIME_COOLDOWN), with.rests().born());
        assertFalse(ErrandSettings.codecOf(ErrandIds.PICK_UP_ITEM).encodeStart(JsonOps.INSTANCE, without).getOrThrow(false, org.junit.jupiter.api.Assertions::fail)
            .getAsJsonObject().getAsJsonObject("rests").has("born"));
        JsonElement written = ErrandSettings.codecOf(ErrandIds.PICK_UP_ITEM).encodeStart(JsonOps.INSTANCE, with).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
        assertEquals("chiikawa:pastime_cooldown",
            written.getAsJsonObject().getAsJsonObject("rests").getAsJsonObject("born").get("from").getAsString());
        assertEquals(with, ErrandSettings.codecOf(ErrandIds.PICK_UP_ITEM).parse(JsonOps.INSTANCE, written).getOrThrow(false, org.junit.jupiter.api.Assertions::fail));
        assertEquals(without, ErrandSettings.codecOf(ErrandIds.PICK_UP_ITEM).parse(JsonOps.INSTANCE,
            ErrandSettings.codecOf(ErrandIds.PICK_UP_ITEM).encodeStart(JsonOps.INSTANCE, without).getOrThrow(false, org.junit.jupiter.api.Assertions::fail)).getOrThrow(false, org.junit.jupiter.api.Assertions::fail));
    }

    @Test
    void aRestAtBirthIsOnTheErrandAndNeverOnASubject() {
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"interrupted\": 0",
            "\"interrupted\": 0, \"born\": { \"ticks\": 100, \"per\": \"subject\" }")).result().isEmpty());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"interrupted\": 0",
            "\"interrupted\": 0, \"born\": 100")).result().isPresent());
    }

    @Test
    void anErrandThatNeverEndsOrHasNoPriorityIsRejected() {
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"max_ticks\": 600", "\"max_ticks\": 0")).result().isEmpty());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"max_ticks\": 600", "\"max_ticks\": -5")).result().isEmpty());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"priority\": 50", "\"priority\": -1")).result().isEmpty());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"score\": 0.6", "\"score\": 1.5")).result().isEmpty());
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"max_ticks\": 600, ", "")).result().isEmpty());
    }

    @Test
    void aPetMayBeToldToGiveUpTheFirstTimeThereIsNoWay() {
        assertEquals(0, ((ErrandTunings.PickUp) parse(ErrandIds.PICK_UP_ITEM, PLANT).getOrThrow(false, org.junit.jupiter.api.Assertions::fail).tuning()).noWayTicks());
        assertEquals(100, ((ErrandTunings.TakePhoto) parse(ErrandIds.TAKE_PHOTO, PHOTO).getOrThrow(false, org.junit.jupiter.api.Assertions::fail).tuning()).noWayTicks());
    }

    @Test
    void aTuningOfTheWrongShapeIsRejected() {
        assertTrue(parse(ErrandIds.PICK_UP_ITEM, PLANT.replace("\"pickup_distance\": 3.0", "\"stand_back\": 3")).result().isEmpty());
        assertTrue(parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("\"no_way_ticks\": 100", "\"no_way_ticks\": -1")).result().isEmpty());
        assertTrue(parse(ErrandIds.STAY, PLANT.replace("\"tuning\": { \"speed\": 0.8, \"pickup_distance\": 3.0, \"max_walk_ticks\": 600, \"no_way_ticks\": 0 }", "\"tuning\": {}")).result().isPresent());
    }

    @Test
    void anErrandYieldsOnlyToAnotherOfItsOwnPriority() {
        Map<ResourceLocation, ErrandSettings<?>> all = decoded(Map.of(ErrandIds.STAY, HARVEST, ErrandIds.PICK_UP_ITEM, PLANT));

        assertEquals(Optional.empty(), all.get(ErrandIds.PICK_UP_ITEM).problemAmong(ErrandIds.PICK_UP_ITEM, all));
        assertEquals(Optional.empty(), all.get(ErrandIds.STAY).problemAmong(ErrandIds.STAY, all));
    }

    @Test
    void anErrandThatYieldsToOneOfAnotherPriorityOrToItselfOrToNoneIsFlagged() {
        Map<ResourceLocation, ErrandSettings<?>> all = decoded(Map.of(
            ErrandIds.STAY, HARVEST.replace("\"priority\": 50", "\"priority\": 30"), ErrandIds.PICK_UP_ITEM, PLANT));
        assertTrue(all.get(ErrandIds.PICK_UP_ITEM).problemAmong(ErrandIds.PICK_UP_ITEM, all).isPresent(), "another priority");

        all = decoded(Map.of(ErrandIds.PICK_UP_ITEM, PLANT));
        assertTrue(all.get(ErrandIds.PICK_UP_ITEM).problemAmong(ErrandIds.PICK_UP_ITEM, all).isPresent(), "not there");

        all = decoded(Map.of(ErrandIds.PICK_UP_ITEM, PLANT.replace("chiikawa:stay", "chiikawa:pick_up_item")));
        assertTrue(all.get(ErrandIds.PICK_UP_ITEM).problemAmong(ErrandIds.PICK_UP_ITEM, all).isPresent(), "itself");
    }

    @Test
    void theLoaderKeepsWhatDecodesAndSaysWhatDoesNot() {
        Loaded loaded = ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.STAY, HARVEST,
            ErrandIds.PICK_UP_ITEM, PLANT.replace("\"max_ticks\": 600", "\"max_ticks\": 0"),
            ErrandIds.TAKE_PHOTO, PHOTO)));

        assertEquals(Set.of(ErrandIds.STAY, ErrandIds.TAKE_PHOTO), loaded.settings().keySet());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("chiikawa:pick_up_item"), loaded.errors().get(0));
        assertTrue(loaded.warnings().isEmpty());
    }

    @Test
    void theLoaderSwitchesOffAnErrandThatYieldsWrongAndNoneOther() {
        Loaded loaded = ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.STAY, HARVEST.replace("\"priority\": 50", "\"priority\": 30"),
            ErrandIds.PICK_UP_ITEM, PLANT)));

        assertEquals(Set.of(ErrandIds.STAY), loaded.settings().keySet(), "harvest itself is fine");
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("yields_to"), loaded.errors().get(0));
    }

    @Test
    void theLoaderSwitchesOffAnErrandWhoseSettingsItsOwnRulesRefuse() {
        Loaded loaded = ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.STAY, HARVEST,
            ErrandIds.TAKE_TASK, TAKE_TASK.replace("\"max_ticks\": 1200", "\"max_ticks\": " + (BoardSlips.RESERVATION_TICKS + 1)))));

        assertEquals(Set.of(ErrandIds.STAY), loaded.settings().keySet());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("chiikawa:take_task") && loaded.errors().get(0).contains("max_ticks"),
            loaded.errors().get(0));
        assertEquals(Set.of(ErrandIds.STAY, ErrandIds.TAKE_TASK), ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.STAY, HARVEST, ErrandIds.TAKE_TASK, TAKE_TASK))).settings().keySet());
    }

    @Test
    void aFileNamedForNoErrandIsWarnedAboutAndSkipped() {
        ResourceLocation stranger = new ResourceLocation("chiikawa", "sulk");
        Map<ResourceLocation, JsonElement> files = files(Map.of(ErrandIds.STAY, HARVEST));
        files.put(stranger, JsonParser.parseString(HARVEST));

        Loaded loaded = ErrandSettingsLoader.load(files);

        assertEquals(Set.of(ErrandIds.STAY), loaded.settings().keySet());
        assertTrue(loaded.errors().isEmpty());
        assertEquals(1, loaded.warnings().size());
        assertTrue(loaded.warnings().get(0).contains("chiikawa:sulk"));
    }

    @Test
    void anErrandWithNoSettingsIsOff() {
        Map<ResourceLocation, ErrandSettings<?>> before = ErrandSettings.all();
        try {
            ErrandSettings.replaceAll(Map.of());

            assertEquals(Optional.empty(), ErrandSettings.of(ErrandIds.PICK_UP_ITEM));
            assertEquals(Optional.empty(), ErrandSettings.of(ErrandIds.PICK_UP_ITEM));
            ErrandSettings.replaceAll(decoded(Map.of(ErrandIds.PICK_UP_ITEM, PLANT)));
            assertTrue(ErrandSettings.of(ErrandIds.PICK_UP_ITEM).isPresent());
        } finally {
            ErrandSettings.replaceAll(before);
        }
    }

    private static void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{}");
    }

    private static RestSpec rest(String json) {
        return RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
    }

    private static String write(RestSpec spec) {
        return RestSpec.CODEC.encodeStart(JsonOps.INSTANCE, spec).getOrThrow(false, org.junit.jupiter.api.Assertions::fail).toString();
    }

    private static com.mojang.serialization.DataResult<? extends ErrandSettings<?>> parse(ResourceLocation errand, String json) {
        Codec<ErrandSettings<?>> codec = ErrandSettings.codecOf(errand);
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static Map<ResourceLocation, ErrandSettings<?>> decoded(Map<ResourceLocation, String> json) {
        Map<ResourceLocation, ErrandSettings<?>> all = new HashMap<>();
        json.forEach((errand, text) -> all.put(errand, parse(errand, text).getOrThrow(false, org.junit.jupiter.api.Assertions::fail)));
        return all;
    }

    private static Map<ResourceLocation, JsonElement> files(Map<ResourceLocation, String> json) {
        Map<ResourceLocation, JsonElement> files = new HashMap<>();
        json.forEach((errand, text) -> files.put(errand, JsonParser.parseString(text)));
        return files;
    }
}
