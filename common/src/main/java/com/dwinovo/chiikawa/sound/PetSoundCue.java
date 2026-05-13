package com.dwinovo.chiikawa.sound;

import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

public record PetSoundCue(Supplier<SoundEvent> sound, float volume, float minPitch, float maxPitch, int weight) {
    public static final float DEFAULT_VOLUME = 1.0F;
    public static final float DEFAULT_PITCH = 1.0F;
    public static final int DEFAULT_WEIGHT = 1;

    public PetSoundCue {
        Objects.requireNonNull(sound, "sound");
        if (volume <= 0.0F) {
            throw new IllegalArgumentException("volume must be positive");
        }
        if (minPitch <= 0.0F || maxPitch <= 0.0F || minPitch > maxPitch) {
            throw new IllegalArgumentException("pitch range must be positive and ordered");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }

    public static PetSoundCue of(Supplier<SoundEvent> sound) {
        return of(sound, DEFAULT_VOLUME, DEFAULT_PITCH, DEFAULT_PITCH, DEFAULT_WEIGHT);
    }

    public static PetSoundCue of(Supplier<SoundEvent> sound, float volume, float minPitch, float maxPitch, int weight) {
        return new PetSoundCue(sound, volume, minPitch, maxPitch, weight);
    }

    public SoundEvent resolve() {
        return sound.get();
    }

    public float samplePitch(RandomSource random) {
        if (minPitch == maxPitch) {
            return minPitch;
        }
        return minPitch + random.nextFloat() * (maxPitch - minPitch);
    }
}
