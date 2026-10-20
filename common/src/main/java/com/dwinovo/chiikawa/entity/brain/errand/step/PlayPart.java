package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.social.PartPlayer;
import com.dwinovo.chiikawa.social.PetInteraction;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * One pet playing its part in a scene, from the moment the two meet until the scene is over: the pose of the part is
 * what the pet shows meanwhile, and it plays the opening beat, the recurring beat each time it comes round, and the
 * closing beat as it lets go, however the part ends (see {@link PartPlayer}). Faces the other pet throughout. Done when
 * the scene has lasted its time, or, for the pet that is visited, when the pet that came over says it is over; gives
 * up if the other pet is gone, has stopped doing what the scene is for, or says it will not play after all.
 * Always interruptible.
 *
 * <p>The scene begins when this step does, for both pets, in the tick the visitor hands over what it brings or the
 * visited pet is told so, and a scene listening to a busker has no hand-over to begin at. So this is where the pet reports
 * it {@link Meeting#PLAYED played}: it is the one thing every scene has, and from then on, however the pet is cut short,
 * the pair leave the scene alone for its cooldown.
 */
public final class PlayPart implements Step {
    private final Meeting meeting;
    private final PetInteraction.Side side;
    private final int ticks;
    @Nullable
    private PartPlayer part;
    private long startedAt;

    /**
     * @param meeting the scene, which says whom
     * @param side the part
     * @param ticks how long the scene lasts
     */
    public PlayPart(Meeting meeting, PetInteraction.Side side, int ticks) {
        this.meeting = meeting;
        this.side = side;
        this.ticks = ticks;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
        part = PartPlayer.begin(LiveHandle.of(handle).pet(), side, startedAt);
        handle.report(Meeting.PLAYED);
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
        part.tick(live.pet(), handle.now());
        if (handle.now() - startedAt >= ticks) {
            meeting.over();
            return StepStatus.DONE;
        }
        return StepStatus.RUNNING;
    }

    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        if (message instanceof Message.SceneOver) {
            meeting.over();
            return StepStatus.DONE;
        }
        if (meeting.withdrawnBy(message)) {
            return StepStatus.gaveUp("withdrawn");
        }
        return StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        part.end(LiveHandle.of(handle).pet());
    }

    @Override
    public String performance() {
        return side.pose().orElse("");
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }

    @Override
    public Set<StepTag> tags() {
        return Set.of(StepTag.IN_SCENE);
    }
}
