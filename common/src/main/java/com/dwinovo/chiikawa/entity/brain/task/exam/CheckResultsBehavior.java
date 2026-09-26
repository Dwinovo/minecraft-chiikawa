package com.dwinovo.chiikawa.entity.brain.task.exam;

import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

/**
 * Walks to the nearest labor board the morning after an exam, stands in front of the
 * results a moment, and hears them: every result it has out.
 */
public class CheckResultsBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.7F;
    /** Manhattan distance to the board the walk counts as arrived; the board itself is solid. */
    private static final int ARRIVE_DISTANCE = 2;
    private static final double READING_DISTANCE_SQR = 3.0 * 3.0;
    /** How long it stands reading the results before it takes them in. */
    private static final int READING_TICKS = 60;
    private static final int MAX_TICKS = 1200;

    private long readUntil;
    private boolean heard;
    private boolean gaveUp;

    public CheckResultsBehavior() {
        super(ImmutableMap.of(
            InitMemory.NEAREST_BOARD.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
        ), MAX_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return PetExams.goesToSeeResults(pet) && TakeExamBehavior.board(level, pet).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        readUntil = -1L;
        heard = false;
        gaveUp = false;
        LaborBoardBlockEntity board = TakeExamBehavior.board(level, pet).orElseThrow();
        BehaviorUtils.setWalkAndLookTargetMemories(pet, board.getBlockPos(), SPEED, ARRIVE_DISTANCE);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !heard && !gaveUp && TakeExamBehavior.board(level, pet).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        LaborBoardBlockEntity board = TakeExamBehavior.board(level, pet).orElseThrow();
        if (readUntil < 0L) {
            if (pet.distanceToSqr(Vec3.atCenterOf(board.getBlockPos())) <= READING_DISTANCE_SQR) {
                pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                pet.getNavigation().stop();
                pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(board.getBlockPos()));
                readUntil = gameTime + READING_TICKS;
            } else if (!pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                // The walk ended short of the board: it hears the results by noon anyway.
                gaveUp = true;
            }
            return;
        }
        if (gameTime >= readUntil) {
            PetExams.hearAllResults(pet);
            heard = true;
        }
    }
}
