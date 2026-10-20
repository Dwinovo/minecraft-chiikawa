package com.dwinovo.chiikawa.entity.brain.errand.run;

/**
 * What a step says after a tick, or after being told something: still at it, finished, or unable to.
 *
 * @param kind which
 * @param reason why it gave up, as a short key; empty otherwise
 */
public record StepStatus(Kind kind, String reason) {
    /** Which. */
    public enum Kind {
        RUNNING,
        DONE,
        GAVE_UP
    }

    public static final StepStatus RUNNING = new StepStatus(Kind.RUNNING, "");
    public static final StepStatus DONE = new StepStatus(Kind.DONE, "");

    /** @return a step that cannot go on, for this reason */
    public static StepStatus gaveUp(String reason) {
        return new StepStatus(Kind.GAVE_UP, reason);
    }
}
