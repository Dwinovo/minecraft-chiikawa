package com.dwinovo.chiikawa.entity.brain.sensor;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.social.InteractionPlan;
import com.dwinovo.chiikawa.social.SocialRules;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

/**
 * Looks at the pets around and now and then thinks of a scene to play with one of them,
 * each scene with its own chance per look (see {@code PetInteraction#chance}). The idea is
 * remembered until the next look; whether the pet acts on it is up to its intents.
 *
 * <p>Reads the pets the vanilla sensor already sees rather than searching the level
 * again. Leaves the pet alone while it is in a scene already, either side of it.
 */
public class PetSocialSensor extends Sensor<AbstractPet> {
    /** Every two seconds: often enough to catch a pet in passing, rarely enough to be cheap. */
    private static final int SCAN_RATE = 40;

    public PetSocialSensor() {
        super(SCAN_RATE);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(InitMemory.INTERACTION_PLAN.get(), MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    @Override
    protected void doTick(ServerLevel level, AbstractPet pet) {
        Brain<AbstractPet> brain = pet.getBrain();
        if (brain.hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get())
                || brain.getMemory(InitMemory.INTERACTION_PLAN.get()).filter(InteractionPlan::engaged).isPresent()) {
            return;
        }
        SocialRules.find(pet, level.getGameTime())
            .ifPresent(plan -> brain.setMemoryWithExpiry(InitMemory.INTERACTION_PLAN.get(), plan, SCAN_RATE));
    }
}
