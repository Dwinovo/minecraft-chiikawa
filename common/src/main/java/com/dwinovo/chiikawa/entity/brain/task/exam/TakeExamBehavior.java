package com.dwinovo.chiikawa.entity.brain.task.exam;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.ExamSeats;
import com.dwinovo.chiikawa.block.LaborBoardBlock;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.qualification.QualificationExam;
import com.dwinovo.chiikawa.qualification.Qualifications;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sits a licence exam: takes a seat in front of the board its owner opened it at, walks to
 * it, sits facing the board over the paper for as long as the licence says, and hands it
 * in. The result is decided as it is handed in and heard the morning after; a pet called
 * away before it hands in gives up its seat and has sat nothing, and can come back to it
 * while the exam is on.
 */
public class TakeExamBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.7F;
    private static final double SEATED_DISTANCE_SQR = 0.8 * 0.8;
    /** Longest the whole exam may take: the walk and the paper. */
    private static final int MAX_TICKS = 2400;

    private PetExams.@Nullable Summons summons;
    private @Nullable BlockPos seat;
    private long writingUntil;
    private boolean handedIn;
    private boolean gaveUp;

    public TakeExamBehavior() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED), MAX_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return PetExams.toSit(pet).flatMap(called -> LaborBoardBlockEntity.at(level, called.board())).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        summons = PetExams.toSit(pet).orElseThrow();
        handedIn = false;
        gaveUp = false;
        writingUntil = -1L;
        LaborBoardBlockEntity board = board(level).orElseThrow();
        OptionalInt place = board.exam().seats().take(pet.getUUID(), gameTime);
        if (place.isEmpty()) {
            gaveUp = true;
            return;
        }
        board.examChanged();
        seat = ExamSeats.position(board.getBlockPos(), board.getBlockState().getValue(LaborBoardBlock.FACING),
            place.getAsInt());
        BehaviorUtils.setWalkAndLookTargetMemories(pet, seat, SPEED, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !handedIn && !gaveUp && board(level).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        LaborBoardBlockEntity board = board(level).orElseThrow();
        board.exam().seats().keep(pet.getUUID(), gameTime);
        if (writingUntil < 0L) {
            if (pet.distanceToSqr(Vec3.atBottomCenterOf(seat)) <= SEATED_DISTANCE_SQR) {
                sitDown(pet, board, gameTime);
            } else if (!pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                // The walk ended short of the seat: it cannot be reached from here.
                gaveUp = true;
            }
            return;
        }
        pet.getLookControl().setLookAt(Vec3.atCenterOf(board.getBlockPos()));
        if (gameTime >= writingUntil) {
            PetExams.handIn(pet, summons.qualification(), board);
            handedIn = true;
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        if (pet.getActivity() == PetActivity.EXAM) {
            pet.setActivity(PetActivity.NONE);
        }
        if (!handedIn) {
            board(level).ifPresent(board -> {
                board.exam().seats().leave(pet.getUUID());
                board.examChanged();
            });
        }
        seat = null;
        summons = null;
    }

    private void sitDown(AbstractPet pet, LaborBoardBlockEntity board, long gameTime) {
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.getNavigation().stop();
        pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(board.getBlockPos()));
        pet.setActivity(PetActivity.EXAM);
        writingUntil = gameTime + Qualifications.get(summons.qualification())
            .map(licence -> QualificationExam.writeTicks(licence, pet.getRandom()))
            .orElse(0);
    }

    private Optional<LaborBoardBlockEntity> board(ServerLevel level) {
        return summons == null ? Optional.empty() : LaborBoardBlockEntity.at(level, summons.board());
    }
}
