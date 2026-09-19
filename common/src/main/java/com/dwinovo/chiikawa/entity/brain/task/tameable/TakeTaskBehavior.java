package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

/**
 * Holds a slip on the nearest labor board, walks over and takes it. If the pet cannot
 * get there or loses the slip, it lets go of the board and rests from it for a while,
 * so it does not keep heading back.
 */
public class TakeTaskBehavior extends Behavior<AbstractPet> {
    private static final int REST_TICKS = 600;
    private static final float SPEED = 0.7F;
    /** Manhattan distance to the board the walk counts as arrived; the board itself is solid. */
    private static final int ARRIVE_DISTANCE = 2;
    private static final double TAKE_DISTANCE_SQR = 3.0 * 3.0;

    private boolean taken;
    private boolean gaveUp;

    public TakeTaskBehavior() {
        super(ImmutableMap.of(
            InitMemory.NEAREST_BOARD.get(), MemoryStatus.VALUE_PRESENT,
            InitMemory.TAKE_TASK_COOLDOWN.get(), MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
        ), BoardSlips.RESERVATION_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return pet.getTask().isEmpty() && board(level, pet).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        taken = false;
        gaveUp = false;
        LaborBoardBlockEntity board = board(level, pet).orElseThrow();
        if (board.reserve(pet)) {
            BehaviorUtils.setWalkAndLookTargetMemories(pet, board.getBlockPos(), SPEED, ARRIVE_DISTANCE);
        } else {
            gaveUp = true;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !taken && !gaveUp && board(level, pet).filter(board -> board.isReservedBy(pet)).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        LaborBoardBlockEntity board = board(level, pet).orElseThrow();
        if (pet.distanceToSqr(Vec3.atCenterOf(board.getBlockPos())) <= TAKE_DISTANCE_SQR) {
            board.claim(pet).ifPresentOrElse(slip -> {
                pet.setTask(slip);
                pet.triggerReaction(PetReaction.HAPPY);
                taken = true;
            }, () -> gaveUp = true);
        } else if (!pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            // The walk ended short of the board: it cannot be reached from here.
            gaveUp = true;
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        if (!taken) {
            board(level, pet).ifPresent(board -> board.release(pet));
            pet.getBrain().setMemoryWithExpiry(InitMemory.TAKE_TASK_COOLDOWN.get(), Unit.INSTANCE, REST_TICKS);
        }
    }

    private static Optional<LaborBoardBlockEntity> board(ServerLevel level, AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.NEAREST_BOARD.get())
            .filter(level::isLoaded)
            .flatMap(pos -> level.getBlockEntity(pos, InitBlockEntities.LABOR_BOARD.get()));
    }
}
