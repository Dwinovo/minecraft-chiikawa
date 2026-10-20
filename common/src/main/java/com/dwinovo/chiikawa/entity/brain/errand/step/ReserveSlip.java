package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Holds the slip the pet would take on a labor board while it walks over, so no other pet heads for the same one.
 * Gives up if the board is gone or has no slip for the pet after all. Done in the tick it takes, so it is never cut
 * half way: the slip is held or it is not.
 *
 * <p>The hold lapses by itself, see {@code BoardSlips.RESERVATION_TICKS}, and is let go of by {@link #letGo} when the
 * errand ends without the slip having been taken.
 */
public final class ReserveSlip implements Step {
    private final GlobalPos board;

    /** @param board the labor board the slip is on */
    public ReserveSlip(GlobalPos board) {
        this.board = board;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<LaborBoardBlockEntity> found = live.board(board);
        if (found.isEmpty()) {
            return StepStatus.gaveUp("gone");
        }
        return found.get().reserve(live.pet()) ? StepStatus.DONE : StepStatus.gaveUp("no_slip");
    }

    /**
     * Gives back any slip the pet still holds on the board, when the errand is off before it was taken. A slip that was
     * taken is no longer held, so nothing is done. Safe to call at any time, whether or not the pet held one.
     */
    public static void letGo(Handle handle, GlobalPos board) {
        LiveHandle live = LiveHandle.of(handle);
        live.board(board).ifPresent(found -> found.release(live.pet()));
    }
}
