package com.dwinovo.chiikawa.entity.brain.task.exam;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.BoardExam;
import com.dwinovo.chiikawa.block.LaborBoardBlock;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.qualification.QualificationExam;
import com.dwinovo.chiikawa.qualification.Qualifications;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sits a licence exam: takes a seat in front of the nearest labor board, walks to it,
 * sits facing the board over the paper for as long as the licence says, and hands it in.
 * The result is decided as it is handed in and heard the morning after; a pet called away
 * before it hands in gives up its seat and has sat nothing.
 */
public class TakeExamBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.7F;
    /** Seats stand this far out from the board's face... */
    private static final int SEAT_DISTANCE = 2;
    /** ...and this far to either side of it, leaving the middle clear for pets taking slips. */
    private static final int[] SEAT_OFFSETS = {-2, -1, 1, 2};
    private static final double SEATED_DISTANCE_SQR = 0.8 * 0.8;
    /** Longest the whole exam may take: the walk and the paper. */
    private static final int MAX_TICKS = 2400;

    private @Nullable ResourceLocation qualification;
    private @Nullable BlockPos seat;
    private long writingUntil;
    private boolean handedIn;
    private boolean gaveUp;

    public TakeExamBehavior() {
        super(ImmutableMap.of(
            InitMemory.NEAREST_BOARD.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
        ), MAX_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return PetExams.toSit(pet).isPresent() && board(level, pet).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        qualification = PetExams.toSit(pet).orElseThrow();
        handedIn = false;
        gaveUp = false;
        writingUntil = -1L;
        LaborBoardBlockEntity board = board(level, pet).orElseThrow();
        OptionalInt place = board.exam().seat(pet.getUUID(), gameTime);
        if (place.isEmpty()) {
            gaveUp = true;
            return;
        }
        board.examChanged();
        seat = seatPos(board, place.getAsInt());
        BehaviorUtils.setWalkAndLookTargetMemories(pet, seat, SPEED, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !handedIn && !gaveUp && board(level, pet).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        LaborBoardBlockEntity board = board(level, pet).orElseThrow();
        board.exam().keep(pet.getUUID(), gameTime);
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
            PetExams.handIn(pet, qualification, board);
            handedIn = true;
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        if (pet.getActivity() == PetActivity.EXAM) {
            pet.setActivity(PetActivity.NONE);
        }
        if (!handedIn) {
            board(level, pet).ifPresent(board -> {
                board.exam().leave(pet.getUUID());
                board.examChanged();
            });
        }
        seat = null;
        qualification = null;
    }

    private void sitDown(AbstractPet pet, LaborBoardBlockEntity board, long gameTime) {
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.getNavigation().stop();
        pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(board.getBlockPos()));
        pet.setActivity(PetActivity.EXAM);
        writingUntil = gameTime + Qualifications.get(qualification)
            .map(licence -> QualificationExam.writeTicks(licence, pet.getRandom()))
            .orElse(0);
    }

    /** The seat at {@code place}: out from the board's face, spread to either side of it. */
    static BlockPos seatPos(LaborBoardBlockEntity board, int place) {
        Direction facing = board.getBlockState().getValue(LaborBoardBlock.FACING);
        return board.getBlockPos().relative(facing, SEAT_DISTANCE)
            .relative(facing.getClockWise(), SEAT_OFFSETS[Math.min(place, BoardExam.SEATS - 1)]);
    }

    static Optional<LaborBoardBlockEntity> board(ServerLevel level, AbstractPet pet) {
        return pet.getBrain().getMemory(InitMemory.NEAREST_BOARD.get())
            .filter(level::isLoaded)
            .flatMap(pos -> level.getBlockEntity(pos, InitBlockEntities.LABOR_BOARD.get()));
    }
}
