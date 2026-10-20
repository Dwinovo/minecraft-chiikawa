package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;

/**
 * Takes the owner a present out of the pet's bag. It starts when the pet is carrying one already, or
 * has one in mind: its owner is about, its last present was long enough ago and there is something
 * in its bag it may give; picking the thing is the behavior's, as it sets off.
 *
 * <p>Its priority is above everything a pet does for itself. A gift is the one errand where the
 * pet wants to be seen doing it, and a cake handed over three chores later is not the
 * same cake: an owner has to be able to connect the present to the pet standing in front
 * of them holding it.
 *
 * <p>Only while the pet is carrying one does it go on: once the present is handed over, or put back
 * in the bag, the errand is done.
 */
public final class GiftOwnerErrand extends LegacyErrand<Plan, ErrandTunings.GiftOwner> {
    private static final Heading TO_OWNER = new Heading(PetView::ownerPos);

    public GiftOwnerErrand() {
        super(ErrandIds.GIFT_OWNER, ErrandCategory.GIFT, ErrandTunings.GiftOwner.CODEC, () -> InitActivity.GIFT_OWNER.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.GiftOwner tuning) {
        return view.carryingGift() || view.presentToGive() ? Check.OK : Check.fail("no_gift");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.GiftOwner tuning) {
        return TO_OWNER;
    }

    @Override
    public Check stillWanted(Plan plan, PetView view, ErrandTunings.GiftOwner tuning) {
        return view.carryingGift() ? Check.OK : Check.fail("no_gift");
    }
}
