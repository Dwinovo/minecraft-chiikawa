package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.entity.AbstractPet;

/**
 * The {@code INTERACTION_RESERVATION} memory, held by the partner of a scene: who is on
 * the way to play it, and which part the partner plays. Written by the pet coming over
 * with an expiry, so a partner whose visitor never arrives goes back to what it was doing;
 * the same way a pet on its way holds a slip on a labor board. Not saved.
 *
 * @param initiator the pet coming over
 * @param side the part the partner plays
 * @param performing whether the initiator has arrived and the scene is being played
 */
public record InteractionReservation(AbstractPet initiator, PetInteraction.Side side, boolean performing) {
    /** @return this reservation, with the scene begun */
    public InteractionReservation perform() {
        return new InteractionReservation(initiator, side, true);
    }

    /** Whether {@code pet} is the one this partner is waiting for. */
    public boolean heldBy(AbstractPet pet) {
        return initiator == pet;
    }
}
