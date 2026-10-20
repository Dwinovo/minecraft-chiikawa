package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk;
import com.dwinovo.chiikawa.entity.brain.errand.run.Executor;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pet is doing, as the systems around it may ask: whether it is at work, seated at its exam,
 * playing a scene. They read this and never reach into the pet's brain or its animation for it, so
 * what they are told is what the pet decided and not something the pet happens to look like.
 */
public interface ErrandView {
    /** @return the errand the pet is on, with its plan and the step it is at */
    Optional<Running> running();

    /**
     * @param now the current game time
     * @return the pet that has asked this one to play a scene and been answered yes, until the scene
     *         is over or the invitation runs out
     */
    Optional<UUID> engagedWith(long now);

    /** @return the id of the errand the pet is on */
    default Optional<ResourceLocation> errand() {
        return running().map(Running::errand);
    }

    /** @return the priority of the errand the pet is on */
    default OptionalInt priority() {
        return running().map(running -> OptionalInt.of(running.priority())).orElse(OptionalInt.empty());
    }

    /** @return whether the pet is on this errand */
    default boolean isOn(ResourceLocation errand) {
        return running().filter(running -> running.errand().equals(errand)).isPresent();
    }

    /** @return whether the step the pet is at is one that sits it down */
    default boolean seated() {
        return has(StepTag.SEATED);
    }

    /** @return whether the step the pet is at is a scene being played */
    default boolean inScene() {
        return has(StepTag.IN_SCENE);
    }

    /** @return whether the step the pet is at is playing music */
    default boolean playingMusic() {
        return has(StepTag.PLAYING_MUSIC);
    }

    private boolean has(StepTag tag) {
        return running().filter(running -> running.tags().contains(tag)).isPresent();
    }

    /** @return the view of a pet whose errands the executor runs and whose invitations the desk answers */
    static ErrandView of(Executor executor, InvitationDesk desk) {
        return new ErrandView() {
            @Override
            public Optional<Running> running() {
                return executor.running();
            }

            @Override
            public Optional<UUID> engagedWith(long now) {
                return desk.engagement(now).map(InvitationDesk.Engagement::initiator);
            }
        };
    }
}
