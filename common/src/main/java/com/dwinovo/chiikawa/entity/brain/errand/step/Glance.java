package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.view.Onlooker;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

/**
 * Looks at someone for as long as the pet's idle habit says: the head turns the way it does for anything else the
 * pet looks at. Done, and not given up, when the time is up or they are gone or have walked out of range: there is
 * nothing wrong with the pet not looking at someone any more. Always interruptible.
 *
 * <p>The gaze is set again every tick, since the brain's own look lasts only so long and is then dropped.
 */
public final class Glance implements Step {
    private final Onlooker kind;
    private final UUID target;
    private float range;
    private long until;

    /**
     * @param kind whom the glance is for, which says which habit of the pet's it is
     * @param target who
     */
    public Glance(Onlooker kind, UUID target) {
        this.kind = kind;
        this.target = target;
    }

    @Override
    public void begin(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        IdleHabits.Glance habit = kind.habit(PetPersonalities.of(live.pet().getType()).idle());
        range = habit.range();
        until = handle.now() + habit.sampleTicks(live.pet().getRandom());
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<Entity> found = live.creature(target).filter(entity -> live.pet().distanceToSqr(entity) <= range * range);
        if (found.isEmpty() || handle.now() >= until) {
            return StepStatus.DONE;
        }
        handle.lookAt(found.get());
        return StepStatus.RUNNING;
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
