package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;
import java.util.Optional;

/**
 * Sits a licence exam at the desk its owner signed it up at: that day, until the exam
 * closes. The desk is where it goes, so it starts only on a desk within the anchor's reach, as a
 * slip is taken, and a pet at heel sits it while its owner stays near the desk. Once it is in
 * its chair it has nowhere left to go.
 *
 * <p>Its priority is above taking a slip or getting on with work: the owner paid for this.
 */
public final class TakeExamErrand extends LegacyErrand<Plan, ErrandTunings.TakeExam> {
    private static final Heading TO_DESK = new Heading(view -> view.seatedAtExam() ? Optional.empty() : view.examDesk());

    public TakeExamErrand() {
        super(ErrandIds.TAKE_EXAM, ErrandCategory.EXAM, ErrandTunings.TakeExam.CODEC, () -> InitActivity.TAKE_EXAM.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.TakeExam tuning) {
        return view.examDesk().isPresent() ? Check.OK : Check.fail("no_desk");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.TakeExam tuning) {
        return TO_DESK;
    }

    /** Kept at until it gets up: a pet that has just handed its paper in is still in its chair. */
    @Override
    public Check stillWanted(Plan plan, PetView view, ErrandTunings.TakeExam tuning) {
        return view.seatedAtExam() ? Check.OK : canStart(view, tuning);
    }
}
