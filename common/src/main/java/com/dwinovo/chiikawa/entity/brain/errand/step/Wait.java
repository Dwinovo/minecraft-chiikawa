package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/** Stands where it is for a while. Always interruptible. */
public final class Wait implements Step {
    private final int ticks;
    private long startedAt;

    /** @param ticks how long to stand */
    public Wait(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
    }

    @Override
    public StepStatus tick(Handle handle) {
        return handle.now() - startedAt >= ticks ? StepStatus.DONE : StepStatus.RUNNING;
    }
}
