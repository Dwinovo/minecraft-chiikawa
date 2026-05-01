package com.dwinovo.chiikawa.anim.state;

/** Coarse movement bucket used by the animation resolver. */
public enum PetLocomotion {
    IDLE,
    WALK,
    RUN;

    /**
     * Keeps the existing movement cutoff for now. WALK is reserved for the
     * next animation pass once dedicated walk cycles exist.
     */
    public static PetLocomotion fromWalkSpeed(float walkSpeed) {
        return walkSpeed > 0.15f ? RUN : IDLE;
    }
}
