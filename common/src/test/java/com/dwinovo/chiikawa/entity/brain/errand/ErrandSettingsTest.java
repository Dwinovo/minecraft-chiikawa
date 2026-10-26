package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
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
        { "priority": 50, "score": 0.6, "max_ticks": 600, "yields_to": ["chiikawa:harvest"], "overlays": [],
          "rests": { "done": 0, "gave_up": { "ticks": 200, "per": "subject" }, "interrupted": 0 },
          "tuning": { "speed": 0.8, "reach_sqr": 3.0, "no_way_ticks": 0 } }""";

    private static final String TAKE_TASK = """
        { "priority": 50, "score": 0.7, "max_ticks": 1200, "yields_to": [], "overlays": [],
          "rests": { "done": 0, "gave_up": 600, "interrupted": 600 },
          "tuning": { "speed": 0.7, "arrive_distance": 2, "take_distance": 3.0 } }""";

    private static final String HARVEST = PLANT.replace("\"yields_to\": [\"chiikawa:harvest\"]", "\"yields_to\": []");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void settingsParseFromTheGeneratedShape() {
        ErrandSettings<?> settings = parse(ErrandIds.TAKE_PHOTO, PHOTO).getOrThrow();

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
            JsonParser.parseString(TAKE_TASK.replace("{ \"priority\"", "{ \"action\": \"busk\", \"priority\""))).getOrThrow();

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

        try (MultiPackResourceManager manager = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(new PathPackResources(
            new PackLocationInfo("test", Component.literal("test"), PackSource.BUILT_IN, Optional.empty()), root)))) {
            assertEquals(Set.of(ErrandIds.HARVEST, ErrandIds.TAKE_PHOTO, ResourceLocation.fromNamespaceAndPath("other", "sulk")),
                ErrandSettingsLoader.errandIds(manager));
        }
    }

    @Test
    void settingsEncodeToWhatTheyParseFrom() {
        decoded(Map.of(ErrandIds.TAKE_PHOTO, PHOTO, ErrandIds.PLANT, PLANT)).forEach((errand, settings) -> {
            JsonElement encoded = ErrandSettings.codecOf(errand).encodeStart(JsonOps.INSTANCE, settings).getOrThrow();

            assertEquals(settings, ErrandSettings.codecOf(errand).parse(JsonOps.INSTANCE, encoded).getOrThrow());
        });
    }

    @Test
    void theListsAndTheVoiceAreWrittenWhenTheyAreNone() {
        ErrandSettings<?> settings = parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("\"yields_to\": [], \"overlays\": [],", "")).getOrThrow();

        var written = ErrandSettings.codecOf(ErrandIds.TAKE_PHOTO).encodeStart(JsonOps.INSTANCE, settings).getOrThrow().getAsJsonObject();

        assertEquals(0, written.getAsJsonArray("yields_to").size(), "a file without them reads as none, and they are written anyway");
        assertEquals(0, written.getAsJsonArray("overlays").size());
        assertFalse(written.has("start_voice"));
    }

    @Test
    void theVoiceOnSettingOffIsRead() {
        ErrandSettings<?> settings = parse(ErrandIds.PLANT, PLANT.replace("\"overlays\": [],", "\"overlays\": [], \"start_voice\": \"hunt\",")).getOrThrow();

        assertEquals(Optional.of(VoiceMoment.HUNT), settings.startVoice());
    }

    @Test
    void anOverlayIsOneOfTheOnesTheModHasAndAnyOtherSwitchesTheErrandOff() {
        ErrandSettings<?> chatting = parse(ErrandIds.PLANT, PLANT.replace("\"overlays\": []", "\"overlays\": [\"chiikawa:idle_chatter\"]")).getOrThrow();

        assertEquals(List.of(Overlays.IDLE_CHATTER), chatting.overlays());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"overlays\": []", "\"overlays\": [\"chiikawa:idle_chater\"]")).isError());
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
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("-1")).isError(), "negative");
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"per\": \"subject\" }")).isError(), "no amount");
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{ \"ticks\": 4, \"per\": \"world\" }")).isError(), "per something else");
    }

    @Test
    void aNumberFromASourceThatDoesNotExistIsRejected() {
        assertTrue(RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{ \"from\": \"chiikawa:camera_retyr\" }")).isError());
        assertTrue(parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("photo_upload_wait", "photo_upload_weight")).isError());
        assertTrue(parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("\"stored\": { \"from\": \"chiikawa:camera_cooldown\" }",
            "\"stored\": { \"from\": \"chiikawa:nowhere\" }")).isError(), "in the reports as well");
    }

    @Test
    void everyNamedSourceCanBePointedAt() {
        for (ResourceLocation name : RestSources.names()) {
            assertEquals(RestSpec.from(name), rest("{ \"from\": \"" + name + "\" }"));
        }
        assertEquals(6, RestSources.names().size());
    }

    @Test
    void anErrandThatGivesUpMustRestAfterwards() {
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("{ \"ticks\": 200, \"per\": \"subject\" }", "0")).isError());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("{ \"ticks\": 200, \"per\": \"subject\" }",
            "{ \"ticks\": 0, \"per\": \"subject\" }")).isError());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"gave_up\": { \"ticks\": 200, \"per\": \"subject\" }, ", "")).isError(),
            "and say how long");
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("{ \"ticks\": 200, \"per\": \"subject\" }",
            "{ \"from\": \"chiikawa:camera_retry\" }")).isSuccess(), "a source is taken to give something");
    }

    @Test
    void theOtherRestsMayBeNothing() {
        assertTrue(parse(ErrandIds.PLANT, PLANT).isSuccess());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"done\": 0", "\"done\": 600")).isSuccess());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"interrupted\": 0", "\"interrupted\": 600")).isSuccess());
    }

    @Test
    void anErrandThatNeverEndsOrHasNoPriorityIsRejected() {
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"max_ticks\": 600", "\"max_ticks\": 0")).isError());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"max_ticks\": 600", "\"max_ticks\": -5")).isError());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"priority\": 50", "\"priority\": -1")).isError());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"score\": 0.6", "\"score\": 1.5")).isError());
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"max_ticks\": 600, ", "")).isError());
    }

    @Test
    void aPetMayBeToldToGiveUpTheFirstTimeThereIsNoWay() {
        assertEquals(0, ((ErrandTunings.BlockWork) parse(ErrandIds.PLANT, PLANT).getOrThrow().tuning()).noWayTicks());
        assertEquals(100, ((ErrandTunings.TakePhoto) parse(ErrandIds.TAKE_PHOTO, PHOTO).getOrThrow().tuning()).noWayTicks());
    }

    @Test
    void aTuningOfTheWrongShapeIsRejected() {
        assertTrue(parse(ErrandIds.PLANT, PLANT.replace("\"reach_sqr\": 3.0", "\"stand_back\": 3")).isError());
        assertTrue(parse(ErrandIds.TAKE_PHOTO, PHOTO.replace("\"no_way_ticks\": 100", "\"no_way_ticks\": -1")).isError());
        assertTrue(parse(ErrandIds.STAY, PLANT.replace("\"tuning\": { \"speed\": 0.8, \"reach_sqr\": 3.0, \"no_way_ticks\": 0 }", "\"tuning\": {}")).isSuccess());
    }

    @Test
    void anErrandYieldsOnlyToAnotherOfItsOwnPriority() {
        Map<ResourceLocation, ErrandSettings<?>> all = decoded(Map.of(ErrandIds.HARVEST, HARVEST, ErrandIds.PLANT, PLANT));

        assertEquals(Optional.empty(), all.get(ErrandIds.PLANT).problemAmong(ErrandIds.PLANT, all));
        assertEquals(Optional.empty(), all.get(ErrandIds.HARVEST).problemAmong(ErrandIds.HARVEST, all));
    }

    @Test
    void anErrandThatYieldsToOneOfAnotherPriorityOrToItselfOrToNoneIsFlagged() {
        Map<ResourceLocation, ErrandSettings<?>> all = decoded(Map.of(
            ErrandIds.HARVEST, HARVEST.replace("\"priority\": 50", "\"priority\": 30"), ErrandIds.PLANT, PLANT));
        assertTrue(all.get(ErrandIds.PLANT).problemAmong(ErrandIds.PLANT, all).isPresent(), "another priority");

        all = decoded(Map.of(ErrandIds.PLANT, PLANT));
        assertTrue(all.get(ErrandIds.PLANT).problemAmong(ErrandIds.PLANT, all).isPresent(), "not there");

        all = decoded(Map.of(ErrandIds.PLANT, PLANT.replace("chiikawa:harvest", "chiikawa:plant")));
        assertTrue(all.get(ErrandIds.PLANT).problemAmong(ErrandIds.PLANT, all).isPresent(), "itself");
    }

    @Test
    void theLoaderKeepsWhatDecodesAndSaysWhatDoesNot() {
        Loaded loaded = ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.HARVEST, HARVEST,
            ErrandIds.PLANT, PLANT.replace("\"max_ticks\": 600", "\"max_ticks\": 0"),
            ErrandIds.TAKE_PHOTO, PHOTO)));

        assertEquals(Set.of(ErrandIds.HARVEST, ErrandIds.TAKE_PHOTO), loaded.settings().keySet());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("chiikawa:plant"), loaded.errors().get(0));
        assertTrue(loaded.warnings().isEmpty());
    }

    @Test
    void theLoaderSwitchesOffAnErrandThatYieldsWrongAndNoneOther() {
        Loaded loaded = ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.HARVEST, HARVEST.replace("\"priority\": 50", "\"priority\": 30"),
            ErrandIds.PLANT, PLANT)));

        assertEquals(Set.of(ErrandIds.HARVEST), loaded.settings().keySet(), "harvest itself is fine");
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("yields_to"), loaded.errors().get(0));
    }

    @Test
    void theLoaderSwitchesOffAnErrandWhoseSettingsItsOwnRulesRefuse() {
        Loaded loaded = ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.HARVEST, HARVEST,
            ErrandIds.TAKE_TASK, TAKE_TASK.replace("\"max_ticks\": 1200", "\"max_ticks\": " + (BoardSlips.RESERVATION_TICKS + 1)))));

        assertEquals(Set.of(ErrandIds.HARVEST), loaded.settings().keySet());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains("chiikawa:take_task") && loaded.errors().get(0).contains("max_ticks"),
            loaded.errors().get(0));
        assertEquals(Set.of(ErrandIds.HARVEST, ErrandIds.TAKE_TASK), ErrandSettingsLoader.load(files(Map.of(
            ErrandIds.HARVEST, HARVEST, ErrandIds.TAKE_TASK, TAKE_TASK))).settings().keySet());
    }

    @Test
    void aFileNamedForNoErrandIsWarnedAboutAndSkipped() {
        ResourceLocation stranger = ResourceLocation.fromNamespaceAndPath("chiikawa", "sulk");
        Map<ResourceLocation, JsonElement> files = files(Map.of(ErrandIds.HARVEST, HARVEST));
        files.put(stranger, JsonParser.parseString(HARVEST));

        Loaded loaded = ErrandSettingsLoader.load(files);

        assertEquals(Set.of(ErrandIds.HARVEST), loaded.settings().keySet());
        assertTrue(loaded.errors().isEmpty());
        assertEquals(1, loaded.warnings().size());
        assertTrue(loaded.warnings().get(0).contains("chiikawa:sulk"));
    }

    @Test
    void anErrandWithNoSettingsIsOff() {
        Map<ResourceLocation, ErrandSettings<?>> before = ErrandSettings.all();
        try {
            ErrandSettings.replaceAll(Map.of());

            assertEquals(Optional.empty(), ErrandSettings.of(ErrandIds.PLANT));
            assertEquals(Optional.empty(), ErrandSettings.of(ErrandIds.PLANT));
            ErrandSettings.replaceAll(decoded(Map.of(ErrandIds.PLANT, PLANT)));
            assertTrue(ErrandSettings.of(ErrandIds.PLANT).isPresent());
        } finally {
            ErrandSettings.replaceAll(before);
        }
    }

    private static void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{}");
    }

    private static RestSpec rest(String json) {
        return RestSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static String write(RestSpec spec) {
        return RestSpec.CODEC.encodeStart(JsonOps.INSTANCE, spec).getOrThrow().toString();
    }

    private static com.mojang.serialization.DataResult<? extends ErrandSettings<?>> parse(ResourceLocation errand, String json) {
        Codec<ErrandSettings<?>> codec = ErrandSettings.codecOf(errand);
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static Map<ResourceLocation, ErrandSettings<?>> decoded(Map<ResourceLocation, String> json) {
        Map<ResourceLocation, ErrandSettings<?>> all = new HashMap<>();
        json.forEach((errand, text) -> all.put(errand, parse(errand, text).getOrThrow()));
        return all;
    }

    private static Map<ResourceLocation, JsonElement> files(Map<ResourceLocation, String> json) {
        Map<ResourceLocation, JsonElement> files = new HashMap<>();
        json.forEach((errand, text) -> files.put(errand, JsonParser.parseString(text)));
        return files;
    }
}
