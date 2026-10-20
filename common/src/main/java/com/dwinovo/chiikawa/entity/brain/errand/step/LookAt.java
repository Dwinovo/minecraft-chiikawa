package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/**
 * Looks at a place or a creature for a while. Gives up if the creature is gone, or, for a look that lasts as long as
 * what is looked at is there, is done when it is. Always interruptible.
 *
 * <p>The gaze is set again every tick, since the brain's own look lasts only so long and is then dropped.
 */
public final class LookAt implements Step {
    private final Goal goal;
    private final int ticks;
    private final boolean untilGone;
    private long startedAt;

    /**
     * @param goal what to look at
     * @param ticks for how long
     */
    public LookAt(Goal goal, int ticks) {
        this(goal, ticks, false);
    }

    private LookAt(Goal goal, int ticks, boolean untilGone) {
        this.goal = goal;
        this.ticks = ticks;
        this.untilGone = untilGone;
    }

    /**
     * A look that goes on for as long as there is something to look at, such as a whistle that holds a pet's interest,
     * and that is the end of it: done when it is gone, and never given up.
     *
     * @param goal what to look at
     */
    public static LookAt untilGone(Goal goal) {
        return new LookAt(goal, Integer.MAX_VALUE, true);
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
    }

    @Override
    public StepStatus tick(Handle handle) {
        if (!goal.lookAt(LiveHandle.of(handle))) {
            return untilGone ? StepStatus.DONE : StepStatus.gaveUp("gone");
        }
        return handle.now() - startedAt >= ticks ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }

    @Override
    public void release(Handle handle) {
        handle.clearLook();
    }
}
