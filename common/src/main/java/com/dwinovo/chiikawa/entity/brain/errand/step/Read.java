package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import net.minecraft.core.GlobalPos;

/**
 * Stands at the chair of an exam desk and reads what is laid on the desk for a while, looking at it. Gives up if
 * the desk is gone. Always interruptible.
 *
 * <p>The gaze is set again every tick, since the brain's own look lasts only so long and is then dropped.
 */
public final class Read implements Step {
    private final GlobalPos desk;
    private final int ticks;
    private long startedAt;

    /**
     * @param desk the desk it reads at
     * @param ticks how long it reads
     */
    public Read(GlobalPos desk, int ticks) {
        this.desk = desk;
        this.ticks = ticks;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
        LiveHandle.of(handle).pet().getNavigation().stop();
    }

    @Override
    public StepStatus tick(Handle handle) {
        if (LiveHandle.of(handle).examDesk(desk).isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        handle.lookAt(desk.pos());
        return handle.now() - startedAt >= ticks ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        handle.clearLook();
    }

    @Override
    public PetActivity pose() {
        return PetActivity.READ_RESULTS;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
