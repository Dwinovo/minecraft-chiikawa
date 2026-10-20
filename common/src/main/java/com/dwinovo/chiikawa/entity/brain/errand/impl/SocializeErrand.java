package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;

/**
 * Goes over to another pet and plays a little scene with it, one the pet has thought of on
 * looking around (see {@code PetSocialSensor}).
 *
 * <p>Ranked above pottering about and below any of the pet's errands and work, so a scene
 * fills idle time and never gets in the way of a job. Like the labor board, it starts only
 * on a partner within the anchor's reach and keeps going while the partner stays within
 * the leash: a pet at heel only plays with pets around its owner's feet.
 */
public final class SocializeErrand extends LegacyErrand<Plan, ErrandTunings.Socialize> {
    private static final Heading TO_PARTNER = new Heading(PetView::socialPartner);

    public SocializeErrand() {
        super(ErrandIds.SOCIALIZE, ErrandCategory.SOCIAL, ErrandTunings.Socialize.CODEC, () -> InitActivity.SOCIALIZE.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.Socialize tuning) {
        return view.socialPartner().isPresent() ? Check.OK : Check.fail("no_partner");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.Socialize tuning) {
        return TO_PARTNER;
    }
}
