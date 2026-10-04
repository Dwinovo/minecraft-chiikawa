package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.whistle.WhistleHeard;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Walks a wild pet to within a few blocks of where a whistle was blown and has it look at
 * whoever is standing there, until the memory of the whistle runs out.
 */
public class AnswerWhistleBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.7F;
    /** How close it comes: near enough to look, not so near as to crowd. */
    private static final int CLOSE_ENOUGH = 3;
    private static final double CLOSE_ENOUGH_SQR = CLOSE_ENOUGH * CLOSE_ENOUGH;
    /** How far from the spot the blower may have wandered and still be the one looked at. */
    private static final double LOOK_RANGE = 8.0;
    /** The memory's own expiry ends this; the limit is only for a pet that never gets there. */
    private static final int MAX_TICKS = 12000;

    public AnswerWhistleBehavior() {
        super(ImmutableMap.of(
            InitMemory.WHISTLE_HEARD.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ), MAX_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return heard(level, pet).isPresent();
    }

    /** Sets off at once, whatever walk the pet was on: the wander it leaves is not this walk. */
    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        heard(level, pet).ifPresent(heard ->
            BehaviorUtils.setWalkAndLookTargetMemories(pet, heard.where().pos(), SPEED, CLOSE_ENOUGH));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return heard(level, pet).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        Optional<WhistleHeard> heard = heard(level, pet);
        if (heard.isEmpty()) {
            return;
        }
        BlockPos spot = heard.get().where().pos();
        Vec3 centre = Vec3.atCenterOf(spot);
        Player player = level.getNearestPlayer(centre.x, centre.y, centre.z, LOOK_RANGE, false);
        pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,
            player != null ? new EntityTracker(player, true) : new BlockPosTracker(centre.add(0.0, 1.0, 0.0)));
        if (pet.distanceToSqr(centre) <= CLOSE_ENOUGH_SQR) {
            pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        } else if (!pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            BehaviorUtils.setWalkAndLookTargetMemories(pet, spot, SPEED, CLOSE_ENOUGH);
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    /** The whistle the pet has set off for, in its own world. */
    private static Optional<WhistleHeard> heard(ServerLevel level, AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.WHISTLE_HEARD.get())
            .filter(heard -> heard.where().dimension().equals(level.dimension()))
            .filter(heard -> heard.from() <= level.getGameTime());
    }
}
