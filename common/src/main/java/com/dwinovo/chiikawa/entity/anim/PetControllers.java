package com.dwinovo.chiikawa.entity.anim;

/**
 * The names of the animation controllers every pet has, in the order they run (a later one
 * is drawn over an earlier one). The renderer registers its controllers under these names
 * and the pet addresses its one-shot animations to them, so both sides read the same list.
 */
public final class PetControllers {
    private PetControllers() {
    }

    /** The state-driven base loop (idle/walk/run/sit/...). */
    public static final String MAIN = "main";
    /**
     * The ambient blink, a constant decorative loop. Pets without a {@code blink} animation
     * get a no-op: the controller stays silent, no per-pet code needed.
     */
    public static final String BLINK = "blink";
    /** One-shot semantic actions the pet is told to do. Runs after the blink, so an action's eye keyframes win. */
    public static final String ACTION = "action";
    /** One-shot emotional reactions; ranks with the action. */
    public static final String REACTION = "reaction";
    /**
     * The mouth opening while the pet says a line, after the reaction whose face arrives in
     * the same tick and would otherwise replace it. It keys only the mouth, so the rest of
     * the face stays as the reaction left it.
     */
    public static final String TALK = "talk";
}
