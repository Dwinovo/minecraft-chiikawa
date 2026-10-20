package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps walking to someone and looking at them, and is done once the pet has arrived at their side: within
 * {@code arrive} blocks counted along the axes, which is how the brain's own mover counts arriving. Gives up if
 * they are gone. It never gives up on a way that cannot be found or on time: the one it follows may be across
 * water or in the air, and the pet is to go on trying until it reaches them or is fetched. Always interruptible.
 *
 * <p>The walk is set again every tick, since the one followed moves.
 */
public final class FollowEntity implements Step {
    private final Goal goal;
    private final float speed;
    private final IntSupplier arrive;

    /**
     * @param goal whom to follow
     * @param speed how fast
     * @param arrive how near, in blocks along the axes, counts as having arrived; asked each tick, since it is a
     *               number of the area a pet acts in and not the errand's
     */
    public FollowEntity(Goal goal, float speed, IntSupplier arrive) {
        this.goal = goal;
        this.speed = speed;
        this.arrive = arrive;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<Vec3> where = goal.where(live);
        if (where.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        int closeEnough = arrive.getAsInt();
        if (live.pet().blockPosition().distManhattan(BlockPos.containing(where.get())) <= closeEnough) {
            return StepStatus.DONE;
        }
        goal.walkTo(live, speed, closeEnough);
        goal.lookAt(live);
        return StepStatus.RUNNING;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
