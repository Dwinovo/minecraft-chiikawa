package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/**
 * Waits by the owner's bed for them to wake, lying quiet as a cat does. Done once the owner is out of bed, whether they
 * woke or left it, and given up on if they are gone from the pet's world. The errand's time limit is what ends the wait
 * of a pet whose owner sleeps on. Always interruptible.
 */
public final class WaitForWaking implements Step {
    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        return LiveHandle.of(handle).owner()
            .map(owner -> owner.isSleeping() ? StepStatus.RUNNING : StepStatus.DONE)
            .orElseGet(() -> StepStatus.gaveUp("gone"));
    }

    @Override
    public PetActivity pose() {
        return PetActivity.SLEEP;
    }
}
