package com.dwinovo.chiikawa.sound;

import com.dwinovo.chiikawa.init.InitSounds;

public final class PetSoundSets {
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

    public static final PetSoundSet USAGI = PetSoundSet.builder()
            .attack(InitSounds.USAGI_ATTACK)
            .hurt(InitSounds.USAGI_INJURED)
            .tame(InitSounds.USAGI_TAME)
            .ambient(PetSoundSet.DEFAULT_AMBIENT_INTERVAL_TICKS, InitSounds.USAGI_AMBIENT)
            .build();

    private PetSoundSets() {
    }
}
