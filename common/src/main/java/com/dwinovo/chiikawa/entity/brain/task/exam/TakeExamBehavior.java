package com.dwinovo.chiikawa.entity.brain.task.exam;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.ExamDeskBlock;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.qualification.QualificationExam;
import com.dwinovo.chiikawa.qualification.Qualifications;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sits a licence exam: walks to the chair of the desk its owner signed it up at, sits on it
 * up against the desk, facing the way the desk faces, writes for as long as the licence
 * says, and hands the paper in, pushing it across the desk before it gets up. The result is decided as
 * it is handed in and heard the morning after; a pet called away before it hands in has sat
 * nothing, and can come back to it while the exam is on.
 */
public class TakeExamBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 0.7F;
    /** Close enough to the chair to sit down on it. */
    private static final double ARRIVED_SQR = 0.8 * 0.8;
    /** Longest the whole exam may take: the walk and the paper. */
    private static final int MAX_TICKS = 2400;
    /** How long it stays in its chair handing the paper in: the {@code hand_in} animation's second. */
    private static final int HAND_IN_TICKS = 20;

    private PetExams.@Nullable Summons summons;
    private long writingUntil;
    private long upAt;
    private boolean handedIn;
    private boolean gaveUp;

    public TakeExamBehavior() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED), MAX_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return PetExams.toSit(pet).flatMap(called -> ExamDeskBlockEntity.at(level, called.desk())).isPresent();
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        summons = PetExams.toSit(pet).orElseThrow();
        handedIn = false;
        gaveUp = false;
        writingUntil = -1L;
        ExamDeskBlockEntity desk = desk(level).orElseThrow();
        BehaviorUtils.setWalkAndLookTargetMemories(pet, desk.chair(), SPEED, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !(handedIn && gameTime >= upAt) && !gaveUp && desk(level).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        ExamDeskBlockEntity desk = desk(level).orElseThrow();
        if (writingUntil < 0L) {
            if (pet.distanceToSqr(Vec3.atBottomCenterOf(desk.chair())) <= ARRIVED_SQR) {
                sitDown(pet, desk, gameTime);
            } else if (!pet.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                // The walk ended short of the chair: it cannot be reached from here.
                gaveUp = true;
            }
            return;
        }
        faceDesk(pet, desk);
        if (!handedIn && gameTime >= writingUntil) {
            PetExams.handIn(pet, summons.qualification(), desk);
            pet.triggerAction(PetAction.HAND_IN);
            handedIn = true;
            upAt = gameTime + HAND_IN_TICKS;
        }
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        if (pet.getActivity() == PetActivity.EXAM) {
            pet.setActivity(PetActivity.NONE);
        }
        summons = null;
    }

    /** Sits on the chair up against the desk, facing it, and starts on the paper. */
    private void sitDown(AbstractPet pet, ExamDeskBlockEntity desk, long gameTime) {
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pet.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        pet.getNavigation().stop();
        Vec3 seat = ExamDeskBlock.seatPoint(desk.getBlockPos(), desk.facing());
        pet.snapTo(seat.x, seat.y, seat.z, desk.facing().toYRot(), 0.0F);
        faceDesk(pet, desk);
        pet.setActivity(PetActivity.EXAM);
        writingUntil = gameTime + Qualifications.get(summons.qualification())
            .map(licence -> QualificationExam.writeTicks(licence, pet.getRandom()))
            .orElse(0);
    }

    /** Keeps the pet square to the desk, body and head, as it writes. */
    private static void faceDesk(AbstractPet pet, ExamDeskBlockEntity desk) {
        float yaw = desk.facing().toYRot();
        pet.setYRot(yaw);
        pet.setYBodyRot(yaw);
        pet.setYHeadRot(yaw);
    }

    private Optional<ExamDeskBlockEntity> desk(ServerLevel level) {
        return summons == null ? Optional.empty() : ExamDeskBlockEntity.at(level, summons.desk());
    }
}
