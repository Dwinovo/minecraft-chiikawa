package com.dwinovo.chiikawa.anim.molang;

/**
 * Tiny variable scope for Molang evaluation. All variables are pre-resolved
 * to integer slot indices at compile time — no string lookup happens during
 * sampling.
 *
 * <p>Slots are populated by the renderer's {@code submit} method just before
 * pose sampling, then the same instance is passed to every
 * {@link MolangNode#eval} call for that frame. The context is intentionally
 * not shared across entities; allocate one per renderer instance and reuse
 * (it's just a {@code double[8]}).
 */
public final class MolangContext {

    public static final int SLOT_ANIM_TIME    = 0;
    public static final int SLOT_GROUND_SPEED = 1;
    public static final int SLOT_HEAD_YAW     = 2;
    public static final int SLOT_HEAD_PITCH   = 3;
    public static final int SLOT_L6_P0        = 4;
    public static final int SLOT_L4_P0        = 5;
    public static final int SLOT_L6_P00       = 6;
    public static final int SLOT_RESERVED     = 7;

    public static final int SLOT_COUNT = 8;

    public final double[] vars = new double[SLOT_COUNT];

    /** Resets all slots to 0. {@code v.L*_P*} have no SET sites in our animations, so 0 is the correct default. */
    public void reset() {
        for (int i = 0; i < SLOT_COUNT; i++) vars[i] = 0.0;
    }

    /**
     * Resolves a Molang variable name (possibly with a prefix like
     * {@code query.}, {@code q.}, {@code v.}, {@code variable.}, or
     * {@code ysm.}) to a slot index, or {@code -1} if unknown.
     */
    public static int resolveSlot(String identifier) {
        // Normalize prefix: q. → query., variable. → v.
        String n = identifier;
        if (n.startsWith("q.")) n = "query." + n.substring(2);
        else if (n.startsWith("variable.")) n = "v." + n.substring("variable.".length());

        return switch (n) {
            case "query.anim_time"    -> SLOT_ANIM_TIME;
            case "query.ground_speed" -> SLOT_GROUND_SPEED;
            case "ysm.head_yaw"       -> SLOT_HEAD_YAW;
            case "ysm.head_pitch"     -> SLOT_HEAD_PITCH;
            case "v.L6_P0"            -> SLOT_L6_P0;
            case "v.L4_P0"            -> SLOT_L4_P0;
            case "v.L6_P00"           -> SLOT_L6_P00;
            default -> -1;
        };
    }
}
