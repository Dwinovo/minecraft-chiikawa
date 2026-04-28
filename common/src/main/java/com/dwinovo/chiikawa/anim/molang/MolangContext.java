package com.dwinovo.chiikawa.anim.molang;

/**
 * Tiny variable scope for Molang evaluation. All variables are pre-resolved
 * to integer slot indices at compile time — no string lookup happens during
 * sampling.
 *
 * <h3>What's in scope</h3>
 * Only the two Molang variables this mod actually drives:
 * <ul>
 *   <li>{@code query.anim_time} — set per channel by
 *       {@link com.dwinovo.chiikawa.anim.runtime.PoseSampler#sample}</li>
 *   <li>{@code query.ground_speed} — set per frame by the renderer from
 *       {@code walkAnimation.speed}</li>
 * </ul>
 *
 * <h3>What's intentionally not in scope</h3>
 * Bedrock animation files exported via Blockbench / Yes Steve Model often
 * carry references to {@code ysm.head_yaw}, {@code ysm.head_pitch}, and
 * Blockbench's IK helper variables ({@code v.L6_P0}, {@code v.L4_P0},
 * {@code v.L6_P00}). None of these have a SET site in any tool we use, and
 * the legacy GeckoLib pipeline silently defaulted them to 0 too — head /
 * ear / tail orientation has always been driven procedurally by the
 * renderer (see {@link com.dwinovo.chiikawa.anim.render.PetBoneInterceptor}),
 * not by Molang. Resolving them to a real slot only opens the door to
 * mis-evaluation (e.g. {@code Root.rotZ = 0.4*ysm.head_yaw} flopping the
 * body sideways). They're left to {@link MolangCompiler}'s soft-fail path,
 * which warns once at load and emits a {@code Const(0)} stand-in.
 *
 * <p>Add a slot here only when the renderer actually has a value to feed.
 *
 * <p>Allocate one context per renderer instance and reuse — it's two
 * doubles, not worth pooling.
 */
public final class MolangContext {

    public static final int SLOT_ANIM_TIME    = 0;
    public static final int SLOT_GROUND_SPEED = 1;

    public static final int SLOT_COUNT = 2;

    public final double[] vars = new double[SLOT_COUNT];

    public void reset() {
        for (int i = 0; i < SLOT_COUNT; i++) vars[i] = 0.0;
    }

    /**
     * Resolves a Molang variable name to a slot index, or {@code -1} if the
     * variable is not in scope (the compiler will emit a {@code Const(0)}).
     *
     * <p>{@code q.} normalizes to {@code query.}; {@code variable.} normalizes
     * to {@code v.}.
     */
    public static int resolveSlot(String identifier) {
        String n = identifier;
        if (n.startsWith("q.")) n = "query." + n.substring(2);
        else if (n.startsWith("variable.")) n = "v." + n.substring("variable.".length());

        return switch (n) {
            case "query.anim_time"    -> SLOT_ANIM_TIME;
            case "query.ground_speed" -> SLOT_GROUND_SPEED;
            default -> -1;
        };
    }
}
