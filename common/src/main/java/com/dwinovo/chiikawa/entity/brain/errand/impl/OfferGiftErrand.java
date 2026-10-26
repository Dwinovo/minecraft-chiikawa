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
import com.dwinovo.chiikawa.entity.brain.errand.step.TakePresent;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.gifting.GiftChoice;
import com.dwinovo.chiikawa.gifting.GiftOutlook;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Takes a present out of the bag to give the owner, when the pet means to: it has one in mind, which something that
 * happened put there and which it has not forgotten, the owner is about and may be given one more today, and it has
 * something to give. The pet is then carrying the present, and {@link GiftOwnerErrand} takes it to the owner.
 *
 * <p>Its priority is above wandering and below work: a pet on its way to a chore does not drop it for a present, it
 * gives it when it has nothing better to do. It is an errand of one step, done in the tick it begins, which is why the
 * present is in the pet's paws before anything else is chosen: the pet that takes it is the pet that gives it.
 *
 * <p>Which of the things it may give is the offers' weights: the choice of one is the decision's, which draws from the
 * pet's random, and this answers the same every time it is asked. The pet rests from it as it rests from giving, in
 * the same memory, so that a pet that has just given one is not ready to think of another.
 */
public final class OfferGiftErrand implements ErrandType<OfferGiftErrand.Offering, ErrandTunings.Untuned> {
    /** What it set out to take: this thing, out of the bag, to the owner wherever they are now. */
    public record Offering(GiftChoice.Pick pick) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.ownerPos();
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.OFFER_GIFT;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.GIFT;
    }

    @Override
    public Codec<ErrandTunings.Untuned> tuningCodec() {
        return ErrandTunings.Untuned.CODEC;
    }

    @Override
    public Offers<Offering> offers(PetView view, ErrandTunings.Untuned tuning) {
        GiftOutlook gift = view.gift();
        if (view.ownerPos().isEmpty()) {
            return Offers.none(Check.fail("owner_unavailable"));
        }
        if (!gift.ready()) {
            return Offers.none(Check.fail("no_present_in_mind"));
        }
        if (gift.leftToday() <= 0) {
            return Offers.none(Check.fail("owner_given_enough"));
        }
        if (gift.picks().isEmpty()) {
            return Offers.none(Check.fail("no_present"));
        }
        return Offers.of(gift.picks().stream().map(pick -> new Offer<>(new Offering(pick), pick.weight())).toList());
    }

    @Override
    public Routine routine(Offering plan, ErrandSettings<ErrandTunings.Untuned> settings) {
        return Routine.of(Routine.step(() -> new TakePresent(plan.pick())));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.giftCooldown();
    }
}
