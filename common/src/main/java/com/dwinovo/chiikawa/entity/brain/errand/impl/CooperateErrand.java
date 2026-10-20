package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;

/**
 * The other side of a scene: while another pet is on its way over, stays where it is,
 * turns to it and plays its part once it arrives.
 *
 * <p>A pet is only asked while it is pottering about, and asking it makes it decide again at
 * once. Its priority is above work's, so something that needs doing does not take it away once it
 * has said yes, and the one coming over does not wait for nobody.
 */
public final class CooperateErrand extends LegacyErrand<Plan, ErrandTunings.Untuned> {
    public CooperateErrand() {
        super(ErrandIds.COOPERATE, ErrandCategory.SOCIAL, ErrandTunings.Untuned.CODEC, () -> InitActivity.COOPERATE.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.Untuned tuning) {
        return view.playingAlong() ? Check.OK : Check.fail("not_asked");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.Untuned tuning) {
        return Heading.NOWHERE;
    }
}
