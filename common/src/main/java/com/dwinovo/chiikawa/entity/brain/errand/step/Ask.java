package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.social.PartPlayer;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Asks someone to praise the pet, as its character asks, and waits for it for as long as the stage says: it holds the pose of
 * the stage with its eyes on them, plays the opening beat as it begins and the recurring one each time it comes round, and
 * wears the icon that tells anyone passing that a click is wanted. Told it was praised, it is glad and done. Never told, it
 * plays the closing beat as the time runs out, which is its turning to a tantrum, and is done as well. Always interruptible,
 * and an interrupted pet makes no scene.
 */
public final class Ask implements Step {
    private final Stage stage;
    private final Goal whom;
    @Nullable
    private PartPlayer part;
    private long startedAt;

    /**
     * @param stage what the character does while it asks, and how long it waits
     * @param whom who it asks, and looks at while it does
     */
    public Ask(Stage stage, Goal whom) {
        this.stage = stage;
        this.whom = whom;
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
        part = PartPlayer.begin(LiveHandle.of(handle).pet(), stage, startedAt);
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        if (!whom.lookAt(live)) {
            return StepStatus.gaveUp("gone");
        }
        part.tick(live.pet(), handle.now());
        if (handle.now() - startedAt < stage.ticks()) {
            return StepStatus.RUNNING;
        }
        part.end(live.pet());
        return StepStatus.DONE;
    }

    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        if (message instanceof Message.Praised) {
            handle.reaction(PetReaction.HAPPY);
            return StepStatus.DONE;
        }
        return StepStatus.RUNNING;
    }

    @Override
    public String performance() {
        return stage.pose().orElse("");
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }

    @Override
    public Set<StepTag> tags() {
        return Set.of(StepTag.ASKING);
    }
}
