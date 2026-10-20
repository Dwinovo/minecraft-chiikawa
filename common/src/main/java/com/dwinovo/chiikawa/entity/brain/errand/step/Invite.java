package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import net.minecraft.resources.ResourceLocation;

/**
 * Asks the pet to be visited to play a scene, with the priority of the errand that asks, and for as long as the
 * asking pet is prepared to wait for it to be played along with. Gives up if the pet is gone. Done in the tick it takes,
 * so it is never cut half way: the pet is asked or it is not.
 */
public final class Invite implements Step {
    private final Meeting meeting;
    private final ResourceLocation scene;
    private final int partnerRank;
    private final int askPriority;
    private final int waitTicks;

    /**
     * @param meeting the scene, which says whom
     * @param scene which scene
     * @param partnerRank which of the scene's partners the asked pet would be
     * @param askPriority how much the asking errand matters: the asked pet is only called away from something of a lower one
     * @param waitTicks for how long the asking pet waits for the asked to be played along with, from now
     */
    public Invite(Meeting meeting, ResourceLocation scene, int partnerRank, int askPriority, int waitTicks) {
        this.meeting = meeting;
        this.scene = scene;
        this.partnerRank = partnerRank;
        this.askPriority = askPriority;
        this.waitTicks = waitTicks;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet partner = meeting.partner(live).orElse(null);
        if (partner == null) {
            return StepStatus.gaveUp("gone");
        }
        handle.post(partner, new Message.Invitation(live.pet().getUUID(), scene, partnerRank, askPriority,
            handle.now() + waitTicks));
        return StepStatus.DONE;
    }
}
