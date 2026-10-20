package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/**
 * Puts the camera up and looks at who is to be photographed, or out at the view, for a while. Gives up if
 * who it is aiming at is gone. Always interruptible.
 */
public final class Aim implements Step {
    private final Goal subject;
    private final int ticks;
    private final PetActivity pose;
    private long startedAt;

    /**
     * @param subject who or what is aimed at
     * @param ticks how long the camera is held up before the shutter
     * @param pose the pose it is held in
     */
    public Aim(Goal subject, int ticks, PetActivity pose) {
        this.subject = subject;
        this.ticks = ticks;
        this.pose = pose;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
        subject.lookAt(LiveHandle.of(handle));
    }

    @Override
    public StepStatus tick(Handle handle) {
        if (!subject.lookAt(LiveHandle.of(handle))) {
            return StepStatus.gaveUp("gone");
        }
        return handle.now() - startedAt >= ticks ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public PetActivity pose() {
        return pose;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
