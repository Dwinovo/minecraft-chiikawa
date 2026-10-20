package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;

/**
 * Walks to the nearest shop and spends some of what the pet has earned on something it
 * likes.
 *
 * <p>Ranked with a day's work and scored above it, below going for a slip, which is what makes the
 * loop a loop: a pet that has just been paid goes and spends a little before starting the next
 * job, and one with nothing in its pocket goes back to work without ever setting off.
 *
 * <p>The shop is where it goes, so it starts only on a shop within the anchor's reach and keeps
 * going while the shop stays within the leash: an owner who told a pet to keep to the garden does
 * not find it in town.
 */
public final class ShopErrand extends LegacyErrand<Plan, ErrandTunings.Shop> {
    private static final Heading TO_SHOP = new Heading(PetView::shopWorthVisiting);

    public ShopErrand() {
        super(ErrandIds.SHOP, ErrandCategory.SHOP, ErrandTunings.Shop.CODEC, () -> InitActivity.SHOP.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.Shop tuning) {
        if (view.shopCoolingDown()) {
            return Check.fail("shop_resting");
        }
        return view.shopWorthVisiting().isPresent() ? Check.OK : Check.fail("nothing_to_buy");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.Shop tuning) {
        return TO_SHOP;
    }
}
