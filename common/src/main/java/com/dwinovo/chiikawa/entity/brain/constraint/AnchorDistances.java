package com.dwinovo.chiikawa.entity.brain.constraint;

/**
 * Tunable anchor distances in blocks. Follow and free values keep the 0.0.9 feel
 * (start following at 7, teleport at 20).
 */
public final class AnchorDistances {
    /** A following pet this far from its owner starts walking back. */
    public static final double FOLLOW_START = 7.0;
    /** Manhattan block distance at which a pet walking to its owner has arrived. */
    public static final int FOLLOW_ARRIVE = 2;
    public static final double FOLLOW_REACH = 12.0;
    public static final double FOLLOW_LEASH = 16.0;
    public static final double FOLLOW_TELEPORT = 20.0;
    public static final double FREE_REACH = 13.0;
    public static final double FREE_LEASH = 20.0;

    private AnchorDistances() {
    }
}
