package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.gifting.Presents;
import net.minecraft.world.entity.player.Player;

/**
 * Gives the owner the present in the pet's paws, see {@link Presents#give}: into their hands where there is room and at
 * their feet where there is not, but given either way, since a present the pet keeps hold of because the owner's
 * inventory was full is a present nobody ever finds out about. Gives up if the owner is gone or the paws are empty. Done
 * in the tick it takes, so it is never cut half way: the present is given or still in the paws, to be put back.
 */
public final class HandOver implements Step {
    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Player owner = live.owner().orElse(null);
        if (owner == null || !Presents.give(live.pet(), owner, Presents.How.HAND)) {
            return StepStatus.gaveUp("no_gift");
        }
        return StepStatus.DONE;
    }
}
