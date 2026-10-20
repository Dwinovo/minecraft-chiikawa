package com.dwinovo.chiikawa.entity.brain.errand;

/**
 * How an errand ended, which is what its rest after it, the decision log and the pet's mood about
 * it are told. Every errand ends in exactly one of these, once.
 */
public sealed interface Outcome {
    /** It did what it set out to do. */
    record Done() implements Outcome {
    }

    /**
     * It could not, or took too long.
     *
     * @param reason why, as a short key ({@code timeout}, {@code unreachable}) for the log
     */
    record GaveUp(String reason) implements Outcome {
    }

    /**
     * Something took the pet away from it.
     *
     * @param cause what, as a short key ({@code higher}, {@code directive}, {@code moved}) for the log
     */
    record Interrupted(String cause) implements Outcome {
    }

    static Outcome done() {
        return new Done();
    }

    static Outcome gaveUp(String reason) {
        return new GaveUp(reason);
    }

    static Outcome interrupted(String cause) {
        return new Interrupted(cause);
    }
}
