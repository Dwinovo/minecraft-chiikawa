package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;

/**
 * Does nothing in particular for as long as the pet's idle habit says, and leaves its gaze to whatever else
 * would have it. Always interruptible.
 */
public final class Pause implements Step {
    private long until;

    @Override
    public void begin(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        until = handle.now() + PetPersonalities.of(live.pet().getType()).idle().rest().sampleTicks(live.pet().getRandom());
    }

    @Override
    public StepStatus tick(Handle handle) {
        return handle.now() >= until ? StepStatus.DONE : StepStatus.RUNNING;
    }
}
