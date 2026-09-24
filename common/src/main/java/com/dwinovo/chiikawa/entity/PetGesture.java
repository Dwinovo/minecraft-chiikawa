package com.dwinovo.chiikawa.entity;

import com.dwinovo.chiikawa.network.PetPayloads.PetGesturePayload;
import com.dwinovo.chiikawa.platform.Services;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A pet making a move once, one named by data — a clap while it listens to music, a pout at
 * the end of a scene — rather than one of the moves the code knows by name
 * ({@code PetAction}, {@code PetReaction}). The server says when; the players who can see
 * the pet are told, as the game tells them of an entity event, and their games play it on
 * the pet's action layer, as a picture in the handbook plays a move it names.
 */
public final class PetGesture {
    private PetGesture() {
    }

    /**
     * Has the pet make the move, for everyone who can see it. Server side; a no-op on the
     * client. A pet whose model has no animation by that name just does not make it.
     *
     * @param animation the move's animation name
     */
    public static void make(AbstractPet pet, String animation) {
        if (!(pet.level() instanceof ServerLevel level)) {
            return;
        }
        // As far as the game keeps the pet in view: its tracking range, in blocks.
        double seen = pet.getType().clientTrackingRange() * SectionPos.SECTION_SIZE;
        Services.NETWORK.sendToPlayersNear(level, pet.position(), seen, new PetGesturePayload(pet.getId(), animation));
    }
}
