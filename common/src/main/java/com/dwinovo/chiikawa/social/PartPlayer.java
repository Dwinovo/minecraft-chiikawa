package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.entity.AbstractPet;

/**
 * One pet playing its part in a scene, from the moment the two meet until the scene is
 * over: it plays the opening beat, the recurring beat each time it comes round, and the
 * closing beat as it lets go. Held by the step the pet plays its part in, the one that
 * came over or the one visited, so both play a part the same way. The part's pose is
 * not its to hold: the step claims it, and the executor shows it.
 */
public final class PartPlayer {
    private final PetInteraction.Side side;
    /** When the recurring beat comes round next, in game time. */
    private long nextBeat;

    private PartPlayer(PetInteraction.Side side, long nextBeat) {
        this.side = side;
        this.nextBeat = nextBeat;
    }

    /**
     * The part begins: the pet plays the opening beat.
     *
     * @param gameTime the current game time
     * @return the part under way, to be ticked while it lasts and ended when it is over
     */
    public static PartPlayer begin(AbstractPet pet, PetInteraction.Side side, long gameTime) {
        side.begin().ifPresent(beat -> beat.play(pet));
        return new PartPlayer(side, side.nowAndThen()
            .map(recurring -> gameTime + recurring.nextWait(pet.getRandom()))
            .orElse(Long.MAX_VALUE));
    }

    /** Plays the recurring beat when it is due. */
    public void tick(AbstractPet pet, long gameTime) {
        side.nowAndThen().filter(recurring -> gameTime >= nextBeat).ifPresent(recurring -> {
            recurring.beat().play(pet);
            nextBeat = gameTime + recurring.nextWait(pet.getRandom());
        });
    }

    /** The part is over, however it ended: the closing beat. */
    public void end(AbstractPet pet) {
        side.end().ifPresent(beat -> beat.play(pet));
    }
}
