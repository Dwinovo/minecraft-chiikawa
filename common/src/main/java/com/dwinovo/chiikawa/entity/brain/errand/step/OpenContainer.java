package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.utils.Deliveries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Opens the container the pet has walked up to, to put its things in: a chest has its lid raised with the sound of
 * opening, and the pet shows it is putting something in. The pet stands where it is while it fills the container. Gives
 * up if the container is gone, or if the pet has nothing to put in it that it would take after all. Done in the tick it takes, so it is never cut half way: the lid is raised, and the
 * {@link Lid} is what lowers it again however the errand ends. It does not look how near the pet is; the walk before
 * it is what brings the pet within reach.
 */
public final class OpenContainer implements Step {
    private final BlockPos container;
    private final Lid lid;
    private final Deliveries deliveries;

    /**
     * @param container the container's block
     * @param lid the lid of that container for this trip, which is told it is raised
     * @param deliveries which containers and which things the delivery is about
     */
    public OpenContainer(BlockPos container, Lid lid, Deliveries deliveries) {
        this.container = container;
        this.lid = lid;
        this.deliveries = deliveries;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        AbstractPet pet = LiveHandle.of(handle).pet();
        if (!(pet.level() instanceof ServerLevel level) || !deliveries.isContainer(level, container)) {
            return StepStatus.gaveUp("gone");
        }
        if (!deliveries.worthVisiting(level, container, pet.getBackpack())) {
            return StepStatus.gaveUp("nothing_to_deliver");
        }
        pet.getNavigation().stop();
        if (lid.exists(level)) {
            handle.action(PetAction.DEPOSIT);
        }
        lid.raise(level);
        return StepStatus.DONE;
    }
}
