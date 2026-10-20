package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.qualification.PetExams;

/**
 * Takes in the results it has read: every result that is out, each up a grade or one more fail to try again after,
 * which the pet shows, says and tells its owner (see {@link PetExams#hearAllResults}). Done in the tick it takes, so
 * it is never cut half way. The pet is still at the desk reading, which is why it holds the pose.
 */
public final class HearResults implements Step {
    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        PetExams.hearAllResults(LiveHandle.of(handle).pet());
        return StepStatus.DONE;
    }

    @Override
    public PetActivity pose() {
        return PetActivity.READ_RESULTS;
    }
}
