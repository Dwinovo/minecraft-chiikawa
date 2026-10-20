package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/**
 * Looks at a place or a creature for a while. Gives up if the creature is gone. Always interruptible.
 *
 * <p>The gaze is set again every tick, since the brain's own look lasts only so long and is then dropped.
 */
public final class LookAt implements Step {
    private final Goal goal;
    private final int ticks;
    private long startedAt;

    /**
     * @param goal what to look at
     * @param ticks for how long
     */
    public LookAt(Goal goal, int ticks) {
        this.goal = goal;
        this.ticks = ticks;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
    }

    @Override
    public StepStatus tick(Handle handle) {
        if (!goal.lookAt(LiveHandle.of(handle))) {
            return StepStatus.gaveUp("gone");
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
