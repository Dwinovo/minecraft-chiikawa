package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import net.minecraft.world.item.ItemStack;

/**
 * Stands where it is and turns to the pet that is coming over, until it arrives. When it does, what it hands over is
 * eaten there and then, a snack being worth what one from the owner is worth, and the pet is as keen on work as the
 * scene makes it: the pet that is visited does that to itself, and no pet does it to another. Done on the scene
 * starting; gives up if the visitor is gone or says it is not coming. The time it waits is the invitation's, which the
 * errand keeps to. Always interruptible.
 */
public final class WaitForGuest implements Step {
    private final Meeting meeting;

    /** @param meeting the scene, which says whom */
    public WaitForGuest(Meeting meeting) {
        this.meeting = meeting;
    }

    @Override
    public void begin(Handle handle) {
        handle.stopWalking();
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet visitor = meeting.partner(live).orElse(null);
        if (visitor == null) {
            return StepStatus.gaveUp("gone");
        }
        handle.lookAt(visitor);
        return StepStatus.RUNNING;
    }

    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        if (message instanceof Message.SceneStart start) {
            eat(LiveHandle.of(handle).pet(), start);
            return StepStatus.DONE;
        }
        if (meeting.withdrawnBy(message)) {
            return StepStatus.gaveUp("withdrawn");
        }
        return StepStatus.RUNNING;
    }

    private static void eat(AbstractPet pet, Message.SceneStart start) {
        ItemStack treat = start.treat();
        if (!treat.isEmpty()) {
            pet.heal(PetInteractHandler.FEED_HEAL);
            pet.playSound(pet.getEatingSound(treat), 1.0F, 1.0F);
        }
        if (start.eagerTicks() > 0) {
            pet.feedDish(start.eagerTicks());
        }
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
