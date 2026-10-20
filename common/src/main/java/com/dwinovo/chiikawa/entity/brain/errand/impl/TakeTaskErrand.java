package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;

/**
 * Walks to the nearest labor board and takes a slip for the pet's job. A pet carries one
 * slip at a time, and goes back for another once it has been paid.
 *
 * <p>The board is where it goes, so it starts only on a board within the anchor's reach, so a
 * following pet takes a slip on the way, and keeps going while the board stays within the leash.
 * A pet that could not get its slip rests from the board for a while, see {@code TAKE_TASK_COOLDOWN}.
 */
public final class TakeTaskErrand extends LegacyErrand<Plan, ErrandTunings.TakeTask> {
    private static final Heading TO_BOARD = new Heading(PetView::offeringBoard);

    public TakeTaskErrand() {
        super(ErrandIds.TAKE_TASK, ErrandCategory.TAKE_TASK, ErrandTunings.TakeTask.CODEC, () -> InitActivity.TAKE_TASK.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.TakeTask tuning) {
        if (view.slip().isPresent()) {
            return Check.fail("has_slip");
        }
        if (view.takeTaskCoolingDown()) {
            return Check.fail("board_resting");
        }
        return view.offeringBoard().isPresent() ? Check.OK : Check.fail("no_slip");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.TakeTask tuning) {
        return TO_BOARD;
    }
}
