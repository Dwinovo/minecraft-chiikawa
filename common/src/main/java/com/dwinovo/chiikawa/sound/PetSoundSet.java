package com.dwinovo.chiikawa.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

public final class PetSoundSet {
    public static final int DEFAULT_AMBIENT_INTERVAL_TICKS = 1200;
    public static final PetSoundSet EMPTY = builder().build();

    private final PetSoundCue attackSound;
    private final PetSoundCue hurtSound;
    private final PetSoundCue deathSound;
    private final PetSoundCue tameSound;
    private final List<PetSoundCue> ambientSounds;
    private final int ambientIntervalTicks;

    private PetSoundSet(Builder builder) {
        this.attackSound = builder.attackSound;
        this.hurtSound = builder.hurtSound;
        this.deathSound = builder.deathSound;
        this.tameSound = builder.tameSound;
        this.ambientSounds = List.copyOf(builder.ambientSounds);
        this.ambientIntervalTicks = builder.ambientIntervalTicks;
    }

    public PetSoundSet(Supplier<SoundEvent> attackSound, Supplier<SoundEvent> hurtSound,
            Supplier<SoundEvent> deathSound, Supplier<SoundEvent> tameSound,
            List<Supplier<SoundEvent>> ambientSounds) {
        this(builder()
                .attack(attackSound)
                .hurt(hurtSound)
                .death(deathSound)
                .tame(tameSound)
                .ambient(DEFAULT_AMBIENT_INTERVAL_TICKS, toCues(ambientSounds)));
    }

    public static Builder builder() {
        return new Builder();
    }

    public PetSoundCue getAttackCue() {
        return attackSound;
    }

    public PetSoundCue getHurtCue() {
        return hurtSound;
    }

    public PetSoundCue getDeathCue() {
        return deathSound;
    }

    public PetSoundCue getTameCue() {
        return tameSound;
    }

    public SoundEvent getAttackSound() {
        return resolve(attackSound);
    }

    public SoundEvent getHurtSound() {
        return resolve(hurtSound);
    }

    public SoundEvent getDeathSound() {
        return resolve(deathSound);
    }

    public SoundEvent getTameSound() {
        return resolve(tameSound);
    }

    public int getAmbientSoundInterval() {
        return ambientIntervalTicks;
    }

    public PetSoundCue pickAmbientCue(RandomSource random) {
        return pickWeighted(ambientSounds, random);
    }

    public SoundEvent pickAmbientSound(RandomSource random) {
        return resolve(pickAmbientCue(random));
    }

    private static PetSoundCue pickWeighted(List<PetSoundCue> cues, RandomSource random) {
        if (cues.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (PetSoundCue cue : cues) {
            totalWeight += cue.weight();
        }

        int roll = random.nextInt(totalWeight);
        for (PetSoundCue cue : cues) {
            roll -= cue.weight();
            if (roll < 0) {
                return cue;
            }
        }
        return cues.get(cues.size() - 1);
    }

    private static SoundEvent resolve(PetSoundCue cue) {
        return cue == null ? null : cue.resolve();
    }

    private static List<PetSoundCue> toCues(List<Supplier<SoundEvent>> sounds) {
        if (sounds == null || sounds.isEmpty()) {
            return List.of();
        }
        List<PetSoundCue> cues = new ArrayList<>();
        for (Supplier<SoundEvent> sound : sounds) {
            if (sound != null) {
                cues.add(PetSoundCue.of(sound));
            }
        }
        return cues;
    }

    private static PetSoundCue cue(Supplier<SoundEvent> sound) {
        return sound == null ? null : PetSoundCue.of(sound);
    }

    public static final class Builder {
        private PetSoundCue attackSound;
        private PetSoundCue hurtSound;
        private PetSoundCue deathSound;
        private PetSoundCue tameSound;
        private final List<PetSoundCue> ambientSounds = new ArrayList<>();
        private int ambientIntervalTicks = DEFAULT_AMBIENT_INTERVAL_TICKS;

        private Builder() {
        }

        public Builder attack(Supplier<SoundEvent> sound) {
            return attack(cue(sound));
        }

        public Builder attack(PetSoundCue cue) {
            this.attackSound = cue;
            return this;
        }

        public Builder hurt(Supplier<SoundEvent> sound) {
            return hurt(cue(sound));
        }

        public Builder hurt(PetSoundCue cue) {
            this.hurtSound = cue;
            return this;
        }

        public Builder death(Supplier<SoundEvent> sound) {
            return death(cue(sound));
        }

        public Builder death(PetSoundCue cue) {
            this.deathSound = cue;
            return this;
        }

        public Builder tame(Supplier<SoundEvent> sound) {
            return tame(cue(sound));
        }

        public Builder tame(PetSoundCue cue) {
            this.tameSound = cue;
            return this;
        }

        @SafeVarargs
        public final Builder ambient(int intervalTicks, Supplier<SoundEvent>... sounds) {
            List<PetSoundCue> cues = new ArrayList<>();
            if (sounds != null) {
                for (Supplier<SoundEvent> sound : sounds) {
                    if (sound != null) {
                        cues.add(PetSoundCue.of(sound));
                    }
                }
            }
            return ambient(intervalTicks, cues);
        }

        public Builder ambient(int intervalTicks, PetSoundCue... cues) {
            List<PetSoundCue> list = new ArrayList<>();
            if (cues != null) {
                for (PetSoundCue cue : cues) {
                    if (cue != null) {
                        list.add(cue);
                    }
                }
            }
            return ambient(intervalTicks, list);
        }

        public Builder ambient(int intervalTicks, List<PetSoundCue> cues) {
            if (intervalTicks <= 0) {
                throw new IllegalArgumentException("ambient interval must be positive");
            }
            this.ambientIntervalTicks = intervalTicks;
            this.ambientSounds.clear();
            if (cues != null) {
                this.ambientSounds.addAll(cues);
            }
            return this;
        }

        public PetSoundSet build() {
            return new PetSoundSet(this);
        }
    }
}
