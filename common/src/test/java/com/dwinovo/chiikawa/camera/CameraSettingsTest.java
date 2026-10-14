package com.dwinovo.chiikawa.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.testing.FixedRandom;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The settings a pack gives a camera, and what the loader does with a file that is wrong. */
class CameraSettingsTest {
    private static final String GOOD = """
        { "film": "chiikawa:film", "owner_range": 64.0, "cooldown_ticks": 12000, "retry_ticks": 600, "width": 320, "height": 240,
          "fov": 70, "subjects": { "owner": 3, "friend": 5, "scenery": 1 }, "aim_ticks": 40, "exposures": 12,
          "posed_chance": 0.25 }""";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void settingsParseFromTheGeneratedShape() {
        CameraSettings settings = CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(GOOD)).getOrThrow();

        assertEquals(new CameraSettings(Identifier.fromNamespaceAndPath("chiikawa", "film"), 64.0, 12000, 600,
            320, 240, 70, new CameraSettings.Subjects(3, 5, 1), 40, 12, 0.25F), settings);
    }

    @Test
    void aFileWithoutAPosedChanceIsPosedHalfTheTimeAndTheChanceIsWrittenAnyway() {
        CameraSettings settings = CameraSettings.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString(GOOD.replace("posed_chance", "something_else"))).getOrThrow();

        assertEquals(0.5F, settings.posedChance());
        assertEquals(0.5F, CameraSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow()
            .getAsJsonObject().get("posed_chance").getAsFloat(), "the chance is not in the file to be changed");
    }

    @Test
    void aFileWithoutExposuresHoldsEightAndWritesThemAnyway() {
        CameraSettings settings = CameraSettings.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString(GOOD.replace("exposures", "something_else"))).getOrThrow();

        assertEquals(8, settings.exposures());
        assertEquals(8, CameraSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow()
            .getAsJsonObject().get("exposures").getAsInt());
    }

    @Test
    void aRollOfNoShotsOrTooManyIsRejected() {
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"exposures\": 12", "\"exposures\": 0"))).isError());
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("\"exposures\": 12", "\"exposures\": 1000"))).isError());
    }

    @Test
    void aPosedChanceOutsideZeroAndOneIsRejected() {
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("0.25", "1.5"))).isError());
        assertTrue(CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            GOOD.replace("0.25", "-0.1"))).isError());
    }

    @Test
    void aPhotoIsPosedByARollBelowTheChance() {
        CameraSettings settings = withPosedChance(0.25F);

        assertTrue(settings.posed(FixedRandom.floats(0.24F)));
        assertFalse(settings.posed(FixedRandom.floats(0.25F)));
        assertFalse(settings.posed(FixedRandom.floats(0.9F)));
    }

    @Test
    void aCameraThatAlwaysOrNeverPosesRollsNothing() {
        // A random source with nothing in it throws if anything is drawn from it.
        assertTrue(withPosedChance(1.0F).posed(FixedRandom.floats()));
        assertFalse(withPosedChance(0.0F).posed(FixedRandom.floats()));
    }

    private static CameraSettings withPosedChance(float chance) {
        CameraSettings settings = CameraSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(GOOD)).getOrThrow();
        return new CameraSettings(settings.film(), settings.ownerRange(), settings.cooldownTicks(), settings.retryTicks(),
            settings.width(), settings.height(), settings.fov(), settings.subjects(), settings.aimTicks(), settings.exposures(), chance);
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
        Identifier good = Identifier.fromNamespaceAndPath("chiikawa", "camera");
        Identifier broken = Identifier.fromNamespaceAndPath("chiikawa", "broken");

        CameraSettingsLoader.Loaded loaded = CameraSettingsLoader.load(Map.of(
            good, JsonParser.parseString(GOOD),
            broken, JsonParser.parseString("{ \"film\": 3 }")));

        assertEquals(1, loaded.settings().size());
        assertTrue(loaded.settings().containsKey(good));
        assertEquals(1, loaded.errors().size());
    }
}
