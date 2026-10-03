package com.dwinovo.chiikawa.entity.brain.task.exam;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
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
 * Walks back to the desk it sat an exam at, the morning after, reads the results laid on it
 * a moment, and hears them: every result it has out.
 */
public class CheckResultsBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.7F;
    /** Close enough to the desk, at its chair, to read what is on it. */
    private static final double READING_DISTANCE_SQR = 1.2 * 1.2;
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
        return PetExams.resultsDesk(pet).flatMap(desk -> ExamDeskBlockEntity.at(level, desk)).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        where = PetExams.resultsDesk(pet).orElseThrow();
        readUntil = -1L;
        heard = false;
        gaveUp = false;
        ExamDeskBlockEntity desk = desk(level).orElseThrow();
        BehaviorUtils.setWalkAndLookTargetMemories(pet, desk.chair(), SPEED, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !heard && !gaveUp && desk(level).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        ExamDeskBlockEntity desk = desk(level).orElseThrow();
        if (readUntil < 0L) {
            if (pet.distanceToSqr(Vec3.atBottomCenterOf(desk.chair())) <= READING_DISTANCE_SQR) {
                pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                pet.getNavigation().stop();
                pet.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(desk.getBlockPos()));
                pet.setActivity(PetActivity.READ_RESULTS);
                readUntil = gameTime + READING_TICKS;
            } else if (!pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                // The walk ended short of the desk: it hears the results by noon anyway.
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

    private Optional<ExamDeskBlockEntity> desk(ServerLevel level) {
        return where == null ? Optional.empty() : ExamDeskBlockEntity.at(level, where);
    }
}
