package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.social.PartPlayer;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Passes the time where it stands, as a pet's character does: holds the pose of the stage for as long as it lasts, and plays
 * its opening beat, the recurring beat each time it comes round, and the closing beat as it lets go, however it ends (see
 * {@link PartPlayer}). Looks at what it came for, the owner it is watching or the creature it came to see, if there is one. Done
 * when the stage has lasted its time, and given up if what it looks at is gone. Always interruptible.
 */
public final class Perform implements Step {
    private final Stage stage;
    private final Optional<Goal> gaze;
    @Nullable
    private PartPlayer part;
    private long startedAt;

    /**
     * @param stage what the character does
     * @param gaze what it looks at while it does, and gives up when it is gone
     */
    public Perform(Stage stage, Optional<Goal> gaze) {
        this.stage = stage;
        this.gaze = gaze;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
        part = PartPlayer.begin(LiveHandle.of(handle).pet(), stage, startedAt);
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        if (gaze.isPresent() && !gaze.get().lookAt(live)) {
            return StepStatus.gaveUp("gone");
        }
        part.tick(live.pet(), handle.now());
        return handle.now() - startedAt >= stage.ticks() ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        part.end(LiveHandle.of(handle).pet());
    }

    @Override
    public String performance() {
        return stage.pose().orElse("");
    }

    @Override
    public boolean holdsGaze() {
        return gaze.isPresent();
    }
}
