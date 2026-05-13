package com.dwinovo.chiikawa.anim.state;

/** Job role as seen by the animation layer. */
public enum PetJobRole {
    NONE,
    FARMER,
    FENCER,
    ARCHER,
    MUSICIAN;

    public static PetJobRole fromId(int jobId) {
        return switch (jobId) {
            case 1 -> FARMER;
            case 2 -> FENCER;
            case 3 -> ARCHER;
            case 4 -> MUSICIAN;
            default -> NONE;
        };
    }
}
