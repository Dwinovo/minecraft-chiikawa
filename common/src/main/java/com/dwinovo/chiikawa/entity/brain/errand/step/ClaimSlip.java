package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Takes down the slip the pet held on a labor board and carries it, pleased. Gives up if the board is gone or the pet
 * no longer holds the slip: its hold lapsed, and whatever the board has now is another pet's to go for. Done in the
 * tick it takes, so it is never cut half way: the pet has the slip and the board has not, or the other way round.
 */
public final class ClaimSlip implements Step {
    private final GlobalPos board;

    /** @param board the labor board the slip is on */
    public ClaimSlip(GlobalPos board) {
        this.board = board;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet pet = live.pet();
        Optional<LaborBoardBlockEntity> found = live.board(board);
        if (found.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        Optional<PetTask> slip = found.filter(held -> held.isReservedBy(pet)).flatMap(held -> held.claim(pet));
        if (slip.isEmpty()) {
            return StepStatus.gaveUp("lost_slip");
        }
        pet.setTask(slip.get());
        handle.reaction(PetReaction.HAPPY);
        return StepStatus.DONE;
    }
}
