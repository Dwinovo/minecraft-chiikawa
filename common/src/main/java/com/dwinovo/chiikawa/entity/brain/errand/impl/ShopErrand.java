package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Buy;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

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
 * not find it in town. It goes on while the shop is still one worth the trip, and what it buys is
 * {@link Buy}'s to settle at the counter.
 *
 * <p>Having bought it rests, so a pet with a full purse does not stand at the counter buying the shop out one
 * cookie at a time. One that found nothing it could buy, or no room for it, rests as long, so that it does not set
 * off again at once for the same. One cut short rests not at all, since nothing was wrong with it. The walk has no
 * limit of its own and none for a pet that cannot find a way: the errand's time limit is what ends it.
 */
public final class ShopErrand implements ErrandType<ShopErrand.Trip, ErrandTunings.Shop> {
    /**
     * What it set out for.
     *
     * @param shop the shop's block, which stays where it is
     */
    public record Trip(GlobalPos shop) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(shop);
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.SHOP;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.SHOP;
    }

    @Override
    public Codec<ErrandTunings.Shop> tuningCodec() {
        return ErrandTunings.Shop.CODEC;
    }

    @Override
    public Offers<Trip> offers(PetView view, ErrandTunings.Shop tuning) {
        return view.shopWorthVisiting()
            .map(shop -> Offers.of(Offer.of(new Trip(shop))))
            .orElseGet(() -> Offers.none(Check.fail("nothing_to_buy")));
    }

    @Override
    public Check stillWanted(Trip plan, PetView view, ErrandTunings.Shop tuning) {
        return view.shopWorthVisiting().isPresent() ? Check.OK : Check.fail("nothing_to_buy");
    }

    @Override
    public Routine routine(Trip plan, ErrandSettings<ErrandTunings.Shop> settings) {
        ErrandTunings.Shop tuning = settings.tuning();
        return Routine.of(
            Routine.step(() -> new WalkTo(new Goal.Place(plan.shop().pos()), tuning.speed(), tuning.arriveDistance(),
                tuning.buyDistance(), settings.maxTicks(), settings.maxTicks())),
            Routine.step(() -> new Buy(plan.shop().pos())));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shopCooldown();
    }
}
