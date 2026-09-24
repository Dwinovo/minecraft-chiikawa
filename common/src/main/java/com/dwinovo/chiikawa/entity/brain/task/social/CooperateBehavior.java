package com.dwinovo.chiikawa.entity.brain.task.social;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.social.InteractionReservation;
import com.dwinovo.chiikawa.social.PartPlayer;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The partner's side of a scene: stays where it is, keeps turned towards the pet coming
 * over, and plays its part once the two have met. Lasts as long as the reservation does;
 * the pet coming over lets go of it when the scene is over, and it runs out on its own
 * when that pet never arrives. Letting go the other way round — this pet being called
 * away — frees the other pet as well.
 */
public class CooperateBehavior extends Behavior<AbstractPet> {
    /** The pet's part, once the one coming over has arrived. */
    private @Nullable PartPlayer part;

    public CooperateBehavior() {
        super(ImmutableMap.of(
            InitMemory.INTERACTION_RESERVATION.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ));
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        part = null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return pet.getBrain().getMemory(InitMemory.INTERACTION_RESERVATION.get())
            .filter(reservation -> reservation.initiator().isAlive())
            .isPresent();
    }

    /** Bounded by the reservation, which runs out by itself. */
    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        Brain<AbstractPet> brain = pet.getBrain();
        InteractionReservation reservation = brain.getMemory(InitMemory.INTERACTION_RESERVATION.get()).orElseThrow();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        BehaviorUtils.lookAtEntity(pet, reservation.initiator());
        if (part != null) {
            part.tick(pet, gameTime);
        } else if (reservation.performing()) {
            part = PartPlayer.begin(pet, reservation.side(), gameTime);
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        if (part != null) {
            part.end(pet);
            part = null;
        }
        pet.getBrain().eraseMemory(InitMemory.INTERACTION_RESERVATION.get());
    }
}
