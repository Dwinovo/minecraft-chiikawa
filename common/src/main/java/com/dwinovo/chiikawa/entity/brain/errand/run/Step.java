package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import java.util.Set;

/**
 * One part of an errand: walk to the crop, pick it, wait. The executor begins a step, ticks it until
 * it says it is done or cannot go on, and goes on to the next. A step does nothing to the pet but
 * through the {@link Handle} it is given, and keeps nothing of the world it was not given a way to
 * undo: what it holds, it lets go of in {@link #release}.
 */
public interface Step {
    /** The step starts; called once, before the first tick. */
    void begin(Handle handle);

    /** @return whether the step is still at it, finished or cannot go on */
    StepStatus tick(Handle handle);

    /**
     * The pet is told something that concerns the errand it is on, such as that the song it was
     * playing has stopped.
     *
     * @return what the step makes of it
     */
    default StepStatus onMessage(Handle handle, Message message) {
        return StepStatus.RUNNING;
    }

    /**
     * @return whether the errand may be ended while this step is under way. Only an action that
     *         is half done is not: movement and waiting always are
     */
    default boolean interruptible() {
        return true;
    }

    /**
     * The step is over, however it ended: finished, given up, or cut short by the errand ending.
     * Called once. Undoes what the step holds in the world, a chest left open or a present in the
     * paws; the pet's walk, gaze and path are cleared by the executor.
     */
    default void release(Handle handle) {
    }

    /** @return the pose the pet holds while this step lasts; the animation follows from it */
    default PetActivity pose() {
        return PetActivity.NONE;
    }

    /** @return the scene animation the pet is in while this step lasts, or empty for none */
    default String performance() {
        return "";
    }

    /**
     * @return whether the step sets the pet's gaze again every tick, so that a passing glance at something else
     *         would be overwritten at once and is left out
     */
    default boolean holdsGaze() {
        return false;
    }

    /** @return what this step makes of the pet, for the systems that ask */
    default Set<StepTag> tags() {
        return Set.of();
    }
}
