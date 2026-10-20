package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.social.PetInteraction;
import java.util.Optional;
import java.util.UUID;

/**
 * A scene two pets play, from the side of one of them: who the other pet is, what it has to be doing for the scene
 * to go on, and whether the scene was played to its end. Made with the routine of a scene and held by its steps, which
 * share it, and the routine's release lets go of it: the other pet is told the scene is over or that this one will
 * not play after all, and a pet that came over lets go of the idea it came for.
 *
 * <p>Nothing of the other pet is kept but its id; it is found again each time, and found gone.
 */
public final class Meeting {
    private final UUID other;
    private final PetInteraction.PartnerState partnerState;
    private final boolean visitor;
    private boolean over;

    private Meeting(UUID other, PetInteraction.PartnerState partnerState, boolean visitor) {
        this.other = other;
        this.partnerState = partnerState;
        this.visitor = visitor;
    }

    /**
     * @param partner the pet that is gone over to
     * @param partnerState what that pet has to be doing for the scene to go on
     * @return the scene of the pet that goes over
     */
    public static Meeting visiting(UUID partner, PetInteraction.PartnerState partnerState) {
        return new Meeting(partner, partnerState, true);
    }

    /**
     * @param visitor the pet that comes over
     * @return the scene of the pet that is visited
     */
    public static Meeting hosting(UUID visitor) {
        return new Meeting(visitor, PetInteraction.PartnerState.IDLE, false);
    }

    /** @return the id of the other pet */
    UUID other() {
        return other;
    }

    /** @return whether the other pet is asked, and so told how the scene ends, or goes on as it is */
    boolean asked() {
        return partnerState == PetInteraction.PartnerState.IDLE;
    }

    /** The scene was played to its end. */
    void over() {
        over = true;
    }

    /** @return the other pet, while it is alive in the pet's world */
    Optional<AbstractPet> partner(LiveHandle live) {
        return live.creature(other).filter(AbstractPet.class::isInstance).map(AbstractPet.class::cast);
    }

    /** @return whether the message is the other pet saying it will not play after all */
    boolean withdrawnBy(Message message) {
        return message instanceof Message.Withdrawn withdrawn && withdrawn.from().equals(other);
    }

    /** @return whether the other pet has stopped doing what the scene was for, such as playing music to listen to */
    boolean lost(AbstractPet partner) {
        return partnerState == PetInteraction.PartnerState.PLAYING_MUSIC && !partner.getMind().errands().playingMusic();
    }

    /**
     * However the errand ends: tells the other pet that the scene is over, if it was played to the end by the pet that went
     * over, or that this pet will not play after all, unless it was played out; and the pet that went over lets go of the
     * idea it went for. The pet that is not asked is told nothing: it carries on.
     *
     * @param handle the pet
     */
    public void end(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        if (visitor) {
            live.pet().getBrain().eraseMemory(InitMemory.INTERACTION_PLAN.get());
        }
        if (!asked()) {
            return;
        }
        Message tell = !over ? new Message.Withdrawn(live.pet().getUUID()) : visitor ? new Message.SceneOver() : null;
        if (tell != null) {
            partner(live).ifPresent(partner -> handle.post(partner, tell));
        }
    }
}
