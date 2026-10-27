package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.ErrandFiles;
import com.dwinovo.chiikawa.data.CharacterPastimeData;
import com.dwinovo.chiikawa.data.PastimeData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettingsLoader;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkFile;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The files of the pastimes: that the ones the mod ships read back as they were written, and that a file that names a cue
 * nobody has, a character who is nobody, or a stage the action cannot do is switched off and not guessed at.
 */
class PastimeFileTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")), ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));

    @BeforeAll
    static void shippedFiles() {
        TestErrands.load();
    }

    /** The file of a pastime as the data generator writes it, to be changed by a case into one that is wrong. */
    private static JsonObject file(ResourceLocation pastime) {
        return ErrandFiles.all().get(pastime).deepCopy().getAsJsonObject();
    }

    private static ErrandSettingsLoader.Loaded load(ResourceLocation id, JsonElement file) {
        return ErrandSettingsLoader.load(Map.of(id, file));
    }

    private static void assertSwitchedOff(ResourceLocation id, JsonElement file, String because) {
        ErrandSettingsLoader.Loaded loaded = load(id, file);

        assertTrue(loaded.works().isEmpty(), id + " was made into an errand");
        assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        assertTrue(loaded.errors().get(0).contains(because), loaded.errors().get(0));
    }

    private static JsonObject stageOf(JsonObject file, ResourceLocation character) {
        return file.getAsJsonObject("cast").getAsJsonObject(character.toString());
    }

    @Test
    void thePastimesTheModShipsAreWorksOfThePastimeCategoryThatReadBackAsTheyWereWritten() {
        assertEquals(PastimeData.ids().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList(),
            Errands.pastimes().stream().map(ErrandType::id).toList(), "in the order of their ids");
        for (ResourceLocation id : PastimeData.ids()) {
            var work = PastimeData.works().get(id);
            WorkFile<?, ?> read = WorkFile.CODEC.parse(JsonOps.INSTANCE, ErrandFiles.all().get(id)).getOrThrow();

            assertEquals(ErrandCategory.PASTIME, work.category(), id.toString());
            // A number provider is not compared by what it holds, so what was read is compared by what it is written as.
            assertEquals(ErrandFiles.all().get(id), WorkFile.CODEC.encodeStart(JsonOps.INSTANCE, read).getOrThrow(), id.toString());
            assertEquals(Errands.get(id).category(), ErrandCategory.PASTIME);
            assertInstanceOf(WorkErrand.class, Errands.get(id));
        }
    }

    @Test
    void aPastimeRestsForItsOwnCooldownAfterItIsDone() {
        for (ResourceLocation id : PastimeData.ids()) {
            JsonObject file = file(id);

            assertEquals("chiikawa:pastime_cooldown", file.getAsJsonObject("rests").getAsJsonObject("done").get("from").getAsString(), id.toString());
            assertTrue(file.getAsJsonObject("tuning").get("cooldown_ticks").getAsInt() > 0, id.toString());
        }
    }

    @Test
    void aCueNobodyHasSwitchesTheFileOff() {
        JsonObject file = file(PastimeData.RAIN_SHELTER);
        JsonObject full = new JsonObject();
        full.addProperty("type", "full_moon");
        file.getAsJsonArray("when").add(full);

        assertSwitchedOff(PastimeData.RAIN_SHELTER, file, "failed to parse");
    }

    @Test
    void aCharacterWhoIsNobodySwitchesTheFileOff() {
        for (String stranger : new String[] {"chiikawa:nobody", "minecraft:pig"}) {
            JsonObject file = file(PastimeData.RAIN_SHELTER);
            file.getAsJsonObject("cast").add(stranger, stageOf(file, PetCharacters.HACHIWARE).deepCopy());

            assertSwitchedOff(PastimeData.RAIN_SHELTER, file, stranger + " is not a character");
        }
    }

    @Test
    void aPastimeWithNobodyInItSwitchesTheFileOff() {
        JsonObject file = file(PastimeData.RAIN_SHELTER);
        file.add("cast", new JsonObject());

        assertSwitchedOff(PastimeData.RAIN_SHELTER, file, "nobody");
    }

    @Test
    void aPastimeThatGoesSomewhereNeedsToSayWhereForEveryoneInIt() {
        JsonObject file = file(PastimeData.RAIN_SHELTER);
        stageOf(file, PetCharacters.HACHIWARE).remove("to");

        assertSwitchedOff(PastimeData.RAIN_SHELTER, file, "visit goes somewhere");
    }

    @Test
    void aPastimeThatStaysWhereItIsCannotSendAnyoneInItAnywhere() {
        JsonObject file = file(PastimeData.RAIN_PLAY);
        JsonObject to = new JsonObject();
        to.addProperty("type", "owner");
        stageOf(file, PetCharacters.USAGI).add("to", to);

        assertSwitchedOff(PastimeData.RAIN_PLAY, file, "perform stays where it is");
    }

    @Test
    void aPastimeThatAsksForPraiseGoesToItsOwnerAndNamesNowhereElse() {
        JsonObject file = file(CharacterPastimeData.ASK_PRAISE);
        JsonObject to = new JsonObject();
        to.addProperty("type", "shelter");
        stageOf(file, PetCharacters.MOMONGA).add("to", to);

        assertSwitchedOff(CharacterPastimeData.ASK_PRAISE, file, "ask_praise goes to its owner");
    }

    @Test
    void aStageThatEndsEarlyEndsOnACueThatIsOneAndAfterAWhile() {
        JsonObject file = file(CharacterPastimeData.COWER);
        JsonObject until = stageOf(file, PetCharacters.CHIIKAWA).getAsJsonObject("until");

        assertEquals("owner", until.getAsJsonObject("cue").get("type").getAsString());
        assertTrue(until.get("after_ticks").getAsInt() > 0);

        until.getAsJsonObject("cue").addProperty("type", "full_moon");
        assertSwitchedOff(CharacterPastimeData.COWER, file, "failed to parse");
    }

    @Test
    void aWorkOfThePastimeCategoryNeedsACastAndNoOtherWorkHasOne() {
        JsonObject bare = file(PastimeData.RAIN_PLAY);
        bare.remove("cast");
        bare.remove("when");
        assertSwitchedOff(PastimeData.RAIN_PLAY, bare, "perform needs a cast");

        JsonObject notAPastime = file(PastimeData.RAIN_PLAY);
        notAPastime.addProperty("category", "work");
        assertSwitchedOff(PastimeData.RAIN_PLAY, notAPastime, "only a pastime has a cast");

        JsonObject music = ErrandFiles.all().get(WorkData.PLAY_MUSIC).deepCopy().getAsJsonObject();
        music.add("cast", file(PastimeData.RAIN_PLAY).get("cast"));
        assertSwitchedOff(WorkData.PLAY_MUSIC, music, "busk takes no cast");
    }

    @Test
    void aPastimeWithNoCuesIsDoneWheneverItsPartIsOpenToThePet() {
        JsonObject file = file(PastimeData.RAIN_PLAY);
        file.add("when", new JsonArray());

        ErrandSettingsLoader.Loaded loaded = load(PastimeData.RAIN_PLAY, file);
        WorkErrand<?, ?> errand = loaded.works().get(PastimeData.RAIN_PLAY);

        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        assertEquals(ErrandCategory.PASTIME, errand.category());
    }

    @Test
    void aPetDoesAPastimeOnlyIfItHasAPartInItAndEveryCueHolds() {
        Pastime shelter = PastimeData.works().get(PastimeData.RAIN_SHELTER).pastime().orElseThrow();
        TestView raining = TestView.at(HOME, TestAnchors.free(HOME)).weather(Weather.RAIN, true);

        assertTrue(shelter.stageFor(raining.species(PetCharacters.HACHIWARE)).isPresent());
        assertTrue(shelter.stageFor(raining.species(PetCharacters.CHIIKAWA)).isPresent());
        assertFalse(shelter.stageFor(raining.species(PetCharacters.USAGI)).isPresent(), "Usagi plays in the rain and goes in out of it for no one");
        assertFalse(shelter.stageFor(TestView.at(HOME, TestAnchors.free(HOME)).species(PetCharacters.HACHIWARE)).isPresent(), "no rain, no shelter");
        assertFalse(shelter.stageFor(raining.weather(Weather.RAIN, false).species(PetCharacters.HACHIWARE)).isPresent(), "already under cover");
    }

    @Test
    void chiikawaHuddlesByItsOwnerAndTheRestGoToTheShelterAndUsagiNeverCries() {
        Pastime shelter = PastimeData.works().get(PastimeData.RAIN_SHELTER).pastime().orElseThrow();
        Pastime play = PastimeData.works().get(PastimeData.RAIN_PLAY).pastime().orElseThrow();

        assertEquals(new Destination.ToOwner(), shelter.cast().get(PetCharacters.CHIIKAWA).to().orElseThrow());
        for (ResourceLocation character : PetCharacters.all()) {
            if (!character.equals(PetCharacters.CHIIKAWA) && !character.equals(PetCharacters.USAGI)) {
                assertEquals(new Destination.ToShelter(), shelter.cast().get(character).to().orElseThrow(), character.toString());
            }
        }
        assertEquals(java.util.Set.of(PetCharacters.USAGI), play.cast().keySet());
        play.cast().get(PetCharacters.USAGI).begin().ifPresent(beat -> assertTrue(beat.voice().isEmpty()));
        for (Stage stage : shelter.cast().values()) {
            assertTrue(stage.begin().flatMap(beat -> beat.voice()).isEmpty() && stage.end().flatMap(beat -> beat.voice()).isEmpty(),
                "a pet in the rain is not made to cry by the data");
        }
    }
}
