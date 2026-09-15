package com.dwinovo.chiikawa.entity.brain.intent;

/**
 * One stage of a long intent (walk somewhere, then do something). The current
 * step lives in {@link RunningIntent} and is advanced with
 * {@link IntentSelector#advanceStep}.
 */
public interface IntentStep {
    String id();

    /** Whether the selector may switch away while this step runs. */
    boolean interruptible();

    /** Ticks after which the step fails; 0 for no limit. */
    int timeoutTicks();
}
