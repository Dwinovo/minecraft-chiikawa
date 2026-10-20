package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;

/**
 * Waits for the pet it asked to say yes or no. Done on a yes; gives up on a no, and when no answer comes in time,
 * which is a pet that is not listening. Always interruptible.
 */
public final class AwaitReply implements Step {
    private final Meeting meeting;
    private final int ticks;
    private long askedAt;

    /**
     * @param meeting the scene, which says whom
     * @param ticks how long to wait for the answer
     */
    public AwaitReply(Meeting meeting, int ticks) {
        this.meeting = meeting;
        this.ticks = ticks;
    }

    @Override
    public void begin(Handle handle) {
        askedAt = handle.now();
    }

    @Override
    public StepStatus tick(Handle handle) {
        return handle.now() - askedAt >= ticks ? StepStatus.gaveUp("no_reply") : StepStatus.RUNNING;
    }

    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        if (message instanceof Message.InvitationReply reply && reply.from().equals(meeting.other())) {
            return reply.accepted() ? StepStatus.DONE : StepStatus.gaveUp("declined");
        }
        return StepStatus.RUNNING;
    }
}
