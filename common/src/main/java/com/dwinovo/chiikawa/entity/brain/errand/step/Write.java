package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.qualification.QualificationExam;
import com.dwinovo.chiikawa.qualification.Qualifications;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes the paper in the chair for as long as the licence says a pet takes over it, which each pet draws for
 * itself as it starts, kept square to the desk, body and head. Gives up if the desk is gone: there is nothing left
 * to write at. Always interruptible, and a pet called away before it hands in has sat nothing.
 */
public final class Write implements Step {
    private final GlobalPos desk;
    private final ResourceLocation qualification;
    private long writingUntil;

    /**
     * @param desk the desk it writes at
     * @param qualification the licence the exam is for
     */
    public Write(GlobalPos desk, ResourceLocation qualification) {
        this.desk = desk;
        this.qualification = qualification;
    }

    @Override
    public void begin(Handle handle) {
        writingUntil = handle.now() + Qualifications.get(qualification)
            .map(licence -> QualificationExam.writeTicks(licence, LiveHandle.of(handle).pet().getRandom()))
            .orElse(0);
    }

    @Override
    public StepStatus tick(Handle handle) {
        Optional<ExamDeskBlockEntity> found = LiveHandle.of(handle).examDesk(desk);
        if (found.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        handle.faceYaw(found.get().facing().toYRot());
        return handle.now() >= writingUntil ? StepStatus.DONE : StepStatus.RUNNING;
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
