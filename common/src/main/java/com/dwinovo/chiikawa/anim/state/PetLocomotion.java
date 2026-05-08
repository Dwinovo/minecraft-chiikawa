package com.dwinovo.chiikawa.anim.state;

/** Coarse movement bucket used by the animation resolver. */
public enum PetLocomotion {
    IDLE,
    WALK,
    RUN;

    /**
     * Maps the entity's normalised walk speed to a movement bucket.
     *
     * <p>Currently we only ship a single moving bucket: any movement above the
     * idle cutoff plays the {@code walk} animation. A separate {@link #RUN}
     * bucket exists in the enum so the resolver can already declare a
     * {@code run → walk → idle} fallback chain, but no walk-speed range maps
     * to it yet — once dedicated run cycles are authored and validated, flip
     * the threshold here to reintroduce {@link #RUN}.
     */
    public static PetLocomotion fromWalkSpeed(float walkSpeed) {
        return walkSpeed > 0.15f ? WALK : IDLE;
    }
}
