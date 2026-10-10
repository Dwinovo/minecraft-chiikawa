package com.dwinovo.chiikawa.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The settings a pack gives a camera, and what the loader does with a file that is wrong. */
class CameraSettingsTest {
    private static final String GOOD = """
        { "film": "chiikawa:film", "owner_range": 64.0, "cooldown_ticks": 12000, "retry_ticks": 600, "width": 320, "height": 240,
          "fov": 70, "subjects": { "owner": 3, "friend": 5, "scenery": 1 }, "aim_ticks": 40 }""";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void settingsParseFromTheGeneratedShape() {
        CameraSettings settings = CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(GOOD)).getOrThrow();

        assertEquals(new CameraSettings(ResourceLocation.fromNamespaceAndPath("chiikawa", "film"), 64.0, 12000, 600,
            320, 240, 70, new CameraSettings.Subjects(3, 5, 1), 40), settings);
    }

    @Test
    void settingsEncodeToWhatTheyParseFrom() {
        CameraSettings settings = CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(GOOD)).getOrThrow();

        assertEquals(settings, CameraSettings.CODEC.parse(JsonOps.INSTANCE,
            CameraSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow()).getOrThrow());
    }

    @Test
    void theWeightsAreLookedUpByKindOfSubject() {
        CameraSettings.Subjects subjects = new CameraSettings.Subjects(3, 5, 1);

        assertEquals(3, subjects.weight(PhotoSubject.OWNER));
        assertEquals(5, subjects.weight(PhotoSubject.FRIEND));
        assertEquals(1, subjects.weight(PhotoSubject.SCENERY));
    }

    @Test
    void settingsThatCannotBeRightAreRejected() {
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"film\": \"chiikawa:film\", ", ""))).isError(), "no film");
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"aim_ticks\": 40", "\"aim_ticks\": 0"))).isError(), "no time to aim");
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"width\": 320", "\"width\": 100000"))).isError(), "a photo too big to keep");
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"fov\": 70", "\"fov\": 170"))).isError(), "a lens too wide to draw");
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"friend\": 5", "\"friend\": -5"))).isError(), "a negative liking");
    }

    @Test
    void aBrokenFileIsSkippedRatherThanTakingTheRestWithIt() {
        ResourceLocation good = ResourceLocation.fromNamespaceAndPath("chiikawa", "camera");
        ResourceLocation broken = ResourceLocation.fromNamespaceAndPath("chiikawa", "broken");

        CameraSettingsLoader.Loaded loaded = CameraSettingsLoader.load(Map.of(
            good, JsonParser.parseString(GOOD),
            broken, JsonParser.parseString("{ \"film\": 3 }")));

        assertEquals(1, loaded.settings().size());
        assertTrue(loaded.settings().containsKey(good));
        assertEquals(1, loaded.errors().size());
    }
}
