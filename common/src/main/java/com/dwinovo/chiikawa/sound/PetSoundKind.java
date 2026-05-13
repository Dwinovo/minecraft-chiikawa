package com.dwinovo.chiikawa.sound;

public enum PetSoundKind {
    AMBIENT(true),
    IDLE_REACTION(true),
    INTERACTION(true),
    ACTIVITY(false),
    TAME(false),
    ATTACK(false),
    HURT(false),
    DEATH(false);

    private final boolean daily;

    PetSoundKind(boolean daily) {
        this.daily = daily;
    }

    public boolean isDaily() {
        return daily;
    }
}
