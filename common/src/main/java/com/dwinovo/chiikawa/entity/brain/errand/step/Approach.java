package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/**
 * Walks up to the pet a scene is to be played with, looking at it as it goes, and is done once the pet is close enough
 * to begin: within the scene's approach distance, and a little more, counted along the axes. The walk stops as soon as
 * it is within the approach distance and a partner shifting its feet should not undo the arrival. Gives up if the other
 * pet is gone or has stopped doing what the scene is for, and if the brain's own mover lets the walk go before the pet is
 * there, which is a partner it cannot get to, and if the walk goes on longer than the other pet is prepared to wait.
 * Always interruptible.
 */
public final class Approach implements Step {
    private final Meeting meeting;
    private final float speed;
    private final int approach;
    private final int margin;
    private final int maxTicks;
    private long startedAt;

    /**
     * @param meeting the scene, which says whom
     * @param speed how fast
     * @param approach how close, in blocks, the walk takes the pet
     * @param margin how much further than that the pet may stand and still begin
     * @param maxTicks the longest the walk lasts before the pet gives up on it
     */
    public Approach(Meeting meeting, float speed, int approach, int margin, int maxTicks) {
        this.meeting = meeting;
        this.speed = speed;
        this.approach = approach;
        this.margin = margin;
        this.maxTicks = maxTicks;
    }

    @Override
    public void begin(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        startedAt = handle.now();
        handle.stopWalking();
        new Goal.Creature(meeting.other()).walkTo(live, speed, approach);
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet partner = meeting.partner(live).orElse(null);
        if (partner == null) {
            return StepStatus.gaveUp("gone");
        }
        if (meeting.lost(partner)) {
            return StepStatus.gaveUp("partner_left");
        }
        handle.lookAt(partner);
        if (live.pet().blockPosition().distManhattan(partner.blockPosition()) <= approach + margin) {
            return StepStatus.DONE;
        }
        if (!handle.walking() || handle.now() - startedAt >= maxTicks) {
            return StepStatus.gaveUp("unreachable");
        }
        return StepStatus.RUNNING;
    }

    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        return meeting.withdrawnBy(message) ? StepStatus.gaveUp("withdrawn") : StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        handle.stopWalking();
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
