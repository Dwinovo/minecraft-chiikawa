package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.remark.PetRemark;
import com.dwinovo.chiikawa.voice.PetVoice;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The remarks the mod ships, and that each character that makes one has the words to: a remark only names a moment, and the
 * moment's lines are the voice's, in every language.
 */
class PetRemarkDataTest {
    private static final PetRemark MISSING = PetRemarkData.all().get(PetRemarkData.MISSING);

    @Test
    void theShippedRemarksSurviveTheTripThroughTheirFiles() {
        PetRemarkData.all().forEach((id, remark) -> assertEquals(remark,
            PetRemark.CODEC.parse(JsonOps.INSTANCE, PetRemark.CODEC.encodeStart(JsonOps.INSTANCE, remark).getOrThrow()).getOrThrow(),
            id::toString));
    }

    @Test
    void everyCharacterHasWordsForTheMomentItsPartInTheMissingRemarkNames() {
        PetVoiceData.all().forEach((pet, voice) -> {
            PetRemark.Part part = MISSING.partOf(pet).orElseThrow(() -> new AssertionError(pet + " has no part"));
            assertFalse(voice.lines().getOrDefault(part.moment(), List.of()).isEmpty(), () -> pet + " has nothing to say at " + part.moment());
        });
    }

    @Test
    void hachiwareSaysItsOwnWayAndEveryoneElseSaysWhatIsShort() {
        ResourceLocation hachiware = ResourceLocation.fromNamespaceAndPath("chiikawa", "hachiware");

        PetVoiceData.all().keySet().forEach(pet -> assertEquals(pet.equals(hachiware) ? VoiceMoment.TROUBLE : VoiceMoment.MISSING,
            MISSING.partOf(pet).orElseThrow().moment(), pet::toString));
    }

    @Test
    void whatIsMissingIsSaidInEveryLanguageWithAPlaceForWhatIsShort() {
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = new HashMap<>();
            LanguageData.addTranslations(locale, words::put);
            PetVoiceData.all().forEach((pet, voice) -> {
                PetRemark.Part part = MISSING.partOf(pet).orElseThrow();
                for (PetVoice.Line line : voice.lines().getOrDefault(part.moment(), List.of())) {
                    String said = words.get(line.text());
                    if (part.moment() == VoiceMoment.MISSING) {
                        assertTrue(said.contains("%s"), () -> locale + " " + line.text() + " has no place for what is missing: " + said);
                    }
                }
            });
        }
    }
}
