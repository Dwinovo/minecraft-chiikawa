package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/**
 * Walks to a place or a creature, looking at it as it goes, and is done once the pet is near enough. Gives
 * up if what it walks to is gone, if the walk goes on too long, or if the pet has been unable to find a
 * way for some time: a path that fails now and then, over ground still being loaded or round a corner, is
 * not a path that cannot be found.
 *
 * <p>The walk is set again every tick, since the brain's own mover drops it whenever it cannot find a path
 * just now, and the pet is to try again. Always interruptible.
 */
public final class WalkTo implements Step {
    private final Goal goal;
    private final float speed;
    private final int closeEnough;
    private final double arriveDistanceSqr;
    private final int maxWalkTicks;
    private final int noWayTicks;
    private long startedAt;

    /**
     * @param goal where to
     * @param speed how fast
     * @param closeEnough how near, in blocks, the brain's mover takes the pet before it counts as there
     * @param arriveDistance how near, in blocks, the pet has to be for the walk to be done
     * @param maxWalkTicks the longest the walk lasts before the pet gives up on it
     * @param noWayTicks how long the pet has to be unable to find a way before it gives up on one
     */
    public WalkTo(Goal goal, float speed, int closeEnough, double arriveDistance, int maxWalkTicks, int noWayTicks) {
        this.goal = goal;
        this.speed = speed;
        this.closeEnough = closeEnough;
        this.arriveDistanceSqr = arriveDistance * arriveDistance;
        this.maxWalkTicks = maxWalkTicks;
        this.noWayTicks = noWayTicks;
    }

    @Override
    public void begin(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        startedAt = handle.now();
        // Nothing left over from an earlier walk, not even that it could not find a way.
        handle.stopWalking();
        goal.walkTo(live, speed, closeEnough);
        goal.lookAt(live);
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<Vec3> where = goal.where(live);
        if (where.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        if (live.pet().distanceToSqr(where.get()) <= arriveDistanceSqr) {
            return StepStatus.DONE;
        }
        if (handle.now() - startedAt >= maxWalkTicks || handle.blockedTicks() >= noWayTicks) {
            return StepStatus.gaveUp("unreachable");
        }
        goal.walkTo(live, speed, closeEnough);
        return StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        handle.stopWalking();
        handle.clearLook();
    }
}
