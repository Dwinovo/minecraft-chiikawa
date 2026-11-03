package com.dwinovo.chiikawa.entity.anim;

import java.util.List;

/**
 * The names of the animation controllers every pet has, and the one place that says in what
 * order they run (a later one is drawn over an earlier one). The renderer registers its
 * controllers under these names, in {@link #ORDER}, and the pet addresses its one-shot
 * animations to them, so both sides read the same list.
 *
 * <p>The order is ambient first, then more and more particular: whatever an animation says
 * about a bone beats what a more general one says about it, and a controller writes only the
 * channels it keys, so the general one shows wherever the particular one leaves a bone alone.
 * The blink is the pet's default for its eyes, and any animation that says what the eyes do,
 * from the sleep that shuts them to a one-shot that squints, wins over it.
 */
public final class PetControllers {
    private PetControllers() {
    }

    /**
     * The ambient blink, a constant decorative loop, under everything so that the eyes blink
     * wherever no animation says otherwise. Pets without a {@code blink} animation get a
     * no-op: the controller stays silent, no per-pet code needed.
     */
    public static final String BLINK = "blink";
    /** The state-driven base loop (idle/walk/run/sit/sleep/...), over the blink so a loop that shuts the eyes keeps them shut. */
    public static final String MAIN = "main";
    /** One-shot semantic actions the pet is told to do. Runs after the base loop, so an action's keyframes win. */
    public static final String ACTION = "action";
    /** One-shot emotional reactions; ranks with the action. */
    public static final String REACTION = "reaction";
    /**
     * The mouth opening while the pet says a line, after the reaction whose face arrives in
     * the same tick and would otherwise replace it. It keys only the mouth, so the rest of
     * the face stays as the reaction left it.
     */
    public static final String TALK = "talk";

    /** Every controller a pet has, in the order they run. */
    public static final List<String> ORDER = List.of(BLINK, MAIN, ACTION, REACTION, TALK);
}
