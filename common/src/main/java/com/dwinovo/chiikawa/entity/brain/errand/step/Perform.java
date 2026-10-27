package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.view.LivePetView;
import com.dwinovo.chiikawa.social.PartPlayer;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Passes the time where it stands, as a pet's character does: holds the pose of the stage for as long as it lasts, and plays
 * its opening beat, the recurring beat each time it comes round, and the closing beat as it lets go, however it ends (see
 * {@link PartPlayer}). Looks at what it came for, the owner it is watching or the creature it came to see, if there is one. Done
 * when the stage has lasted its time, or earlier if the stage has something that ends it and that holds, and given up if what
 * it looks at is gone. Always interruptible.
 */
public final class Perform implements Step {
    /** How often a stage with something that ends it asks: a few times a second is soon enough for anyone to be noticed. */
    private static final int ASK_EVERY = 10;

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
        long lasted = handle.now() - startedAt;
        return lasted >= stage.ticks() || over(live, lasted) ? StepStatus.DONE : StepStatus.RUNNING;
    }

    /** @return whether what ends the stage early holds, asked once the stage has lasted long enough and every few ticks */
    private boolean over(LiveHandle live, long lasted) {
        return stage.until().filter(until -> lasted >= until.afterTicks() && lasted % ASK_EVERY == 0)
            .filter(until -> until.cue().holds(LivePetView.of(live.pet()))).isPresent();
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
