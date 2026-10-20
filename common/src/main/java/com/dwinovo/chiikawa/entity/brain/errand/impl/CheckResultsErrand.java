package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;

/**
 * Goes back to the exam desk it sat an exam at, the morning after, to see how it did. A
 * pet that cannot get there by noon hears its results wherever it is instead; see
 * {@link com.dwinovo.chiikawa.qualification.PetExams#upkeep}.
 *
 * <p>Its priority is the exam's: nothing else is on its mind that morning.
 */
public final class CheckResultsErrand extends LegacyErrand<Plan, ErrandTunings.CheckResults> {
    private static final Heading TO_DESK = new Heading(PetView::resultsDesk);

    public CheckResultsErrand() {
        super(ErrandIds.CHECK_RESULTS, ErrandCategory.EXAM, ErrandTunings.CheckResults.CODEC,
            () -> InitActivity.CHECK_RESULTS.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.CheckResults tuning) {
        return view.resultsDesk().isPresent() ? Check.OK : Check.fail("no_desk");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.CheckResults tuning) {
        return TO_DESK;
    }
}
