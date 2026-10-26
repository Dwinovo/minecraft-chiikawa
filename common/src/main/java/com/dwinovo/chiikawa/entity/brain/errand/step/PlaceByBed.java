package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.gifting.Presents;
import net.minecraft.world.entity.player.Player;

/**
 * Leaves the owner the present in the pet's paws by their bed, as a cat leaves its morning gift: when the owner has woken
 * from a sleep of the length a night's is, and at the time of day they wake from one, just after dawn. Woken at any
 * other time, the owner is given nothing here and the pet gives up, to put the present back. Done in the tick it
 * takes, so it is never cut half way: the present is on the floor or still in the paws.
 */
public final class PlaceByBed implements Step {
    private final float morningFrom;
    private final float morningTo;
    private final int sleptTicks;

    /**
     * @param morningFrom the time of day after which the owner has woken in the morning, as the level's clock reads it
     * @param morningTo the time of day before which they have
     * @param sleptTicks how long the owner has to have slept to have woken, as the game counts it
     */
    public PlaceByBed(float morningFrom, float morningTo, int sleptTicks) {
        this.morningFrom = morningFrom;
        this.morningTo = morningTo;
        this.sleptTicks = sleptTicks;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Player owner = live.owner().orElse(null);
        if (owner == null) {
            return StepStatus.gaveUp("gone");
        }
        float timeOfDay = owner.level().getTimeOfDay(1.0F);
        if (owner.getSleepTimer() < sleptTicks || timeOfDay <= morningFrom || timeOfDay >= morningTo) {
            return StepStatus.gaveUp("not_morning");
        }
        return Presents.give(live.pet(), owner, Presents.How.BY_THE_BED) ? StepStatus.DONE : StepStatus.gaveUp("no_gift");
    }
}
