package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.qualification.PetExams;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Hands the paper in across the desk: the result is decided now, kept with the pet until the morning and by the
 * desk to show then, and the owner hears it is done (see {@link PetExams#handIn}). Gives up if the desk is gone.
 * Done in the tick it takes, so it is never cut half way: the paper is handed in, licence and desk together, or not.
 *
 * <p>The pet is still in its chair, which is why it holds the pose and stays seated.
 */
public final class HandIn implements Step {
    private final GlobalPos desk;
    private final ResourceLocation qualification;

    /**
     * @param desk the desk it hands the paper in at
     * @param qualification the licence the exam is for
     */
    public HandIn(GlobalPos desk, ResourceLocation qualification) {
        this.desk = desk;
        this.qualification = qualification;
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
        handle.faceYaw(found.get().facing().toYRot());
        PetExams.handIn(live.pet(), qualification, found.get());
        handle.action(PetAction.HAND_IN);
        return StepStatus.DONE;
    }

    @Override
    public PetActivity pose() {
        return PetActivity.EXAM;
    }

    @Override
    public Set<StepTag> tags() {
        return Set.of(StepTag.SEATED);
    }
}
