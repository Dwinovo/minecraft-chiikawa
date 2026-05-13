package com.dwinovo.chiikawa.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class PetSoundSetTest {
    private static final Supplier<SoundEvent> TEST_SOUND = () -> null;

    @Test
    void emptySetHasDefaultAmbientIntervalAndNoAmbientCue() {
        assertEquals(PetSoundSet.DEFAULT_AMBIENT_INTERVAL_TICKS, PetSoundSet.EMPTY.getAmbientSoundInterval());
        assertNull(PetSoundSet.EMPTY.pickAmbientCue(RandomSource.create(1L)));
    }

    @Test
    void builderStoresSingleCues() {
        PetSoundSet set = PetSoundSet.builder()
                .attack(TEST_SOUND)
                .build();

        assertSame(TEST_SOUND, set.getAttackCue().sound());
        assertNull(set.getAttackSound());
    }

    @Test
    void ambientPoolUsesConfiguredIntervalAndCue() {
        PetSoundCue cue = PetSoundCue.of(TEST_SOUND, 0.7F, 0.9F, 1.1F, 2);
        PetSoundSet set = PetSoundSet.builder()
                .ambient(320, cue)
                .build();

        assertEquals(320, set.getAmbientSoundInterval());
        assertSame(cue, set.pickAmbientCue(RandomSource.create(1L)));
    }
}
