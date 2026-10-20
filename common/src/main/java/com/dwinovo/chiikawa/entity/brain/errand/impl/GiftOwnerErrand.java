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
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.HandOver;
import com.dwinovo.chiikawa.entity.brain.errand.step.TakePresent;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.gifting.GiftingSettings;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Takes the owner a present out of the pet's bag. It starts when the pet is carrying one already, or
 * has one in mind: its owner is about, its last present was long enough ago (the rest the ledger keeps for
 * this errand) and there is something in its bag it may give. Picking the thing is {@link TakePresent}'s, as it
 * sets off.
 *
 * <p>Its priority is above everything a pet does for itself. A gift is the one errand where the
 * pet wants to be seen doing it, and a cake handed over three chores later is not the
 * same cake: an owner has to be able to connect the present to the pet standing in front
 * of them holding it.
 *
 * <p>The present is drawn from the pet's bag as it sets off, one of the thing, and carried in its
 * paws to the owner and handed across, into their hands where there is room and at their feet where
 * there is not. If the errand is dropped on the way, because the owner has gone or the pet was told to
 * sit, it goes back in the bag. Only while the pet is carrying one does it go on.
 *
 * <p>Once a present is handed over the pet thinks of no other for as long as the gifting settings say. One
 * given up on rests too, for half a minute, so that a present put back is not taken out again at once.
 * An errand cut short rests not at all, since nothing was wrong with it.
 */
public final class GiftOwnerErrand implements ErrandType<GiftOwnerErrand.Delivery, ErrandTunings.GiftOwner> {
    /** What it set out to do: take the present to the owner, wherever they are now. */
    public record Delivery() implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.ownerPos();
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.GIFT_OWNER;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.GIFT;
    }

    @Override
    public Codec<ErrandTunings.GiftOwner> tuningCodec() {
        return ErrandTunings.GiftOwner.CODEC;
    }

    @Override
    public Offers<Delivery> offers(PetView view, ErrandTunings.GiftOwner tuning) {
        return view.carryingGift() || view.presentToGive()
            ? Offers.of(Offer.of(new Delivery())) : Offers.none(Check.fail("no_gift"));
    }

    @Override
    public Check stillWanted(Delivery plan, PetView view, ErrandTunings.GiftOwner tuning) {
        return view.carryingGift() ? Check.OK : Check.fail("no_gift");
    }

    /**
     * The walk has no limit of its own and none for a pet that cannot find a way: a long way round is long enough
     * to cross a garden, and the errand's time limit is what ends it.
     */
    @Override
    public Routine routine(Delivery plan, ErrandSettings<ErrandTunings.GiftOwner> settings) {
        ErrandTunings.GiftOwner tuning = settings.tuning();
        return Routine.of(
            Routine.step(TakePresent::new),
            Routine.step(() -> new WalkTo(new Goal.Owner(), tuning.speed(), tuning.arriveDistance(), tuning.reach(),
                settings.maxTicks(), settings.maxTicks())),
            Routine.step(HandOver::new)).releasing(TakePresent::putBack);
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.giftCooldown();
    }

    @Override
    public Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Map.of(RestSources.GIFTING_COOLDOWN,
            context -> GiftingSettings.current().map(GiftingSettings::cooldownTicks).orElse(0));
    }
}
