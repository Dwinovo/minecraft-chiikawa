package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Holds a pose for a while, looking at something if there is something to look at: the camera is not put
 * away before the picture is drawn, and a paper is not left on the desk the moment it is handed in. Done early
 * if what it looks at is gone, since there is nothing left to hold the pose for, and when the pet is told
 * something that ends it, such as that the picture is back. Always interruptible.
 */
public final class Hold implements Step {
    /** An ending for a hold that only time ends. */
    public static final Function<Message, StepStatus> TIME_ALONE = message -> StepStatus.RUNNING;

    private final int ticks;
    private final PetActivity pose;
    private final Set<StepTag> tags;
    private final Optional<Goal> gaze;
    private final Function<Message, StepStatus> ending;
    private long startedAt;

    /**
     * @param ticks how long to hold it
     * @param pose the pose
     * @param tags what the pose makes of the pet, such as that it is sat down
     * @param gaze what to look at meanwhile, if anything
     * @param ending what the step makes of what the pet is told while it holds the pose
     */
    public Hold(int ticks, PetActivity pose, Set<StepTag> tags, Optional<Goal> gaze, Function<Message, StepStatus> ending) {
        this.ticks = ticks;
        this.pose = pose;
        this.tags = Set.copyOf(tags);
        this.gaze = gaze;
        this.ending = ending;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
    }

    @Override
    public StepStatus tick(Handle handle) {
        if (gaze.isPresent() && !gaze.get().lookAt(LiveHandle.of(handle))) {
            return StepStatus.DONE;
        }
        return handle.now() - startedAt >= ticks ? StepStatus.DONE : StepStatus.RUNNING;
    }

    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        return ending.apply(message);
    }

    @Override
    public PetActivity pose() {
        return pose;
    }

    @Override
    public boolean holdsGaze() {
        return gaze.isPresent();
    }

    @Override
    public Set<StepTag> tags() {
        return tags;
    }
}
