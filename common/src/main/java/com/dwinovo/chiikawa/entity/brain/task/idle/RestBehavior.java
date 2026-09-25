package com.dwinovo.chiikawa.entity.brain.task.idle;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;

/**
 * Does nothing in particular for as long as the pet's habit says, as vanilla's
 * {@code DoNothing} does for a time fixed when it is made.
 */
public final class RestBehavior extends Behavior<AbstractPet> {
    private long until;

    public RestBehavior() {
        super(Map.of(), IdleHabits.LONGEST_TICKS);
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        until = gameTime + PetPersonalities.of(pet.getType()).idle().rest().sampleTicks(pet.getRandom());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return gameTime < until;
    }
}
