package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import net.minecraft.server.level.ServerLevel;

/**
 * Closes the container the pet has put its things in: a chest has its lid lowered with the sound of closing. Done in the
 * tick it takes, so it is never cut half way, and a pet that is cut short before it has closed it has it closed for it by
 * the routine's release.
 */
public final class CloseContainer implements Step {
    private final Lid lid;

    /** @param lid the lid of the container for this trip */
    public CloseContainer(Lid lid) {
        this.lid = lid;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (pet.level() instanceof ServerLevel level) {
            lid.lower(level);
        }
        return StepStatus.DONE;
    }
}
