package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.block.ExamDeskBlock;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;

/**
 * Sits the pet down on the chair of the exam desk it has walked up to: put on the seat, up against the desk and
 * square to it, with the way it came forgotten. Gives up if the desk is gone. Done in the tick it takes, so it is
 * never cut half way.
 *
 * <p>The pet is seated from here on, but the pose is the next step's, so that it is drawn sitting in the tick it
 * is put on the chair and not before.
 */
public final class SitDown implements Step {
    private final GlobalPos desk;

    /** @param desk the desk whose chair it is */
    public SitDown(GlobalPos desk) {
        this.desk = desk;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<ExamDeskBlockEntity> found = live.examDesk(desk);
        if (found.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        // The walk that brought it is over, but the mover may still hold the path it was on.
        live.pet().getNavigation().stop();
        ExamDeskBlockEntity chair = found.get();
        handle.snapTo(ExamDeskBlock.seatPoint(chair.getBlockPos(), chair.facing()), chair.facing().toYRot());
        return StepStatus.DONE;
    }

    @Override
    public Set<StepTag> tags() {
        return Set.of(StepTag.SEATED);
    }
}
