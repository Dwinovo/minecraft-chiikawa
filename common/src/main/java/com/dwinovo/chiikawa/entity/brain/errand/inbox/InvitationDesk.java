package com.dwinovo.chiikawa.entity.brain.errand.inbox;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Where a pet answers another pet's invitation to a scene, and remembers having said yes. It says
 * yes only to a pet that asks with something that matters more than what it is doing, so a pet at
 * work is never pulled off it for a scene and one pottering about is: that is the whole of who is
 * free to be asked. It then waits for the asker for as long as the asker said it would, and no longer.
 */
public final class InvitationDesk {
    /**
     * Having said yes: the pet that asked, and what it asked for.
     *
     * @param initiator who asked
     * @param scene which scene
     * @param partnerRank which of the scene's partners this pet is
     * @param expiresAt the game time after which the asker no longer waits
     */
    public record Engagement(UUID initiator, ResourceLocation scene, int partnerRank, long expiresAt) {
    }

    @Nullable
    private Engagement engagement;

    /**
     * Weighs an invitation and, if it is accepted, remembers it.
     *
     * <p>Accepted when the pet's owner lets it play, it is not resting from this scene with this pet,
     * nobody else is on the way to it, and what the asker asks with matters more than what the pet
     * is doing, or the pet is doing nothing.
     *
     * @param invitation the invitation
     * @param permitted whether the constraints on the pet allow it a scene
     * @param resting whether it is resting from this scene with this pet
     * @param runningPriority the priority of the errand it is on, or none
     * @param now the current game time
     * @return whether the pet says yes
     */
    public boolean consider(Message.Invitation invitation, boolean permitted, boolean resting,
            OptionalInt runningPriority, long now) {
        boolean taken = engagement(now).filter(held -> !held.initiator().equals(invitation.initiator())).isPresent();
        boolean free = runningPriority.isEmpty() || invitation.askPriority() > runningPriority.getAsInt();
        if (!permitted || resting || taken || !free || invitation.expiresAt() <= now) {
            return false;
        }
        engagement = new Engagement(invitation.initiator(), invitation.scene(), invitation.partnerRank(),
            invitation.expiresAt());
        return true;
    }

    /**
     * @param now the current game time
     * @return what the pet said yes to, unless the asker has stopped waiting; a lapsed one is as good as
     *         forgotten, and is replaced by the next
     */
    public Optional<Engagement> engagement(long now) {
        return Optional.ofNullable(engagement).filter(held -> now < held.expiresAt());
    }

    /**
     * The pet that asked is not coming. Forgets it if it is the one the pet had said yes to.
     *
     * @param from who
     * @return whether it was the one the pet had said yes to
     */
    public boolean withdrawn(UUID from) {
        if (engagement != null && engagement.initiator().equals(from)) {
            engagement = null;
            return true;
        }
        return false;
    }

    /** Forgets the engagement, as when the scene is over or the pet is removed from the world. */
    public void clear() {
        engagement = null;
    }
}
