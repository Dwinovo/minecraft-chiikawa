package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What the pets have to say, as the series draws them. A moment a character would let pass
 * in silence has no lines; these are the moments nobody does.
 */
class PetVoiceDataTest {
    @Test
    void everyPetHasSomethingToSayWhenItPassesAnExam() {
        PetVoiceData.all().forEach((pet, voice) -> assertFalse(
            voice.lines().getOrDefault(VoiceMoment.EXAM_PASS, List.of()).isEmpty(),
            () -> pet + " passes its exam without a word"));
    }
}
