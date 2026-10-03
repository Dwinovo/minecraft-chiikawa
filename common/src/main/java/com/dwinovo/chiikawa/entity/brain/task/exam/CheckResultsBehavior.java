package com.dwinovo.chiikawa.entity.brain.task.exam;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Walks back to the board it sat an exam at, the morning after, stands in front of the
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

    private @Nullable GlobalPos where;
    private long readUntil;
    private boolean heard;
    private boolean gaveUp;

    public CheckResultsBehavior() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED), MAX_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return PetExams.resultsBoard(pet).flatMap(board -> LaborBoardBlockEntity.at(level, board)).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        where = PetExams.resultsBoard(pet).orElseThrow();
        readUntil = -1L;
        heard = false;
        gaveUp = false;
        BehaviorUtils.setWalkAndLookTargetMemories(pet, where.pos(), SPEED, ARRIVE_DISTANCE);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !heard && !gaveUp && board(level).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        LaborBoardBlockEntity board = board(level).orElseThrow();
        if (readUntil < 0L) {
            if (pet.distanceToSqr(Vec3.atCenterOf(board.getBlockPos())) <= READING_DISTANCE_SQR) {
                pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                pet.getNavigation().stop();
                pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(board.getBlockPos()));
                pet.setActivity(PetActivity.READ_RESULTS);
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

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        if (pet.getActivity() == PetActivity.READ_RESULTS) {
            pet.setActivity(PetActivity.NONE);
        }
        where = null;
    }

    private Optional<LaborBoardBlockEntity> board(ServerLevel level) {
        return where == null ? Optional.empty() : LaborBoardBlockEntity.at(level, where);
    }
}
