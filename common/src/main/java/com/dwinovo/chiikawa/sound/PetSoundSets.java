package com.dwinovo.chiikawa.sound;

import com.dwinovo.chiikawa.init.InitSounds;

public final class PetSoundSets {
    private static final float USAGI_ATTACK_VOLUME = 0.35F;

    public static final PetSoundSet CHIIKAWA = PetSoundSet.builder()
            .hurt(InitSounds.CHIIKAWA_INJURED)
            .tame(InitSounds.CHIIKAWA_TAME)
            .build();

    public static final PetSoundSet HACHIWARE = PetSoundSet.builder()
            .hurt(InitSounds.HACHIWARE_INJURED)
            .tame(InitSounds.HACHIWARE_TAME)
            .build();

    public static final PetSoundSet KURIMANJU = PetSoundSet.builder()
            .tame(InitSounds.KURIMANJU_TAME)
            .build();

    public static final PetSoundSet MOMONGA = PetSoundSet.builder()
            .hurt(InitSounds.MOMONGA_INJURED)
            .tame(InitSounds.MOMONGA_TAME)
            .build();

    public static final PetSoundSet RAKKO = PetSoundSet.builder()
            .tame(InitSounds.RAKKO_TAME)
            .build();

    public static final PetSoundSet SHISA = PetSoundSet.builder()
            .tame(InitSounds.SHISA_TAME)
            .build();

    /**
     * Usagi's clips are mastered much hotter than the other characters'. The
     * ambient voice is left out on purpose ({@code usagi/ambient} stays
     * registered for later use) and the attack cue is played quieter.
     */
    public static final PetSoundSet USAGI = PetSoundSet.builder()
            .attack(PetSoundCue.of(InitSounds.USAGI_ATTACK, USAGI_ATTACK_VOLUME,
                    PetSoundCue.DEFAULT_PITCH, PetSoundCue.DEFAULT_PITCH, PetSoundCue.DEFAULT_WEIGHT))
            .hurt(InitSounds.USAGI_INJURED)
            .tame(InitSounds.USAGI_TAME)
            .build();

    private PetSoundSets() {
    }
}
